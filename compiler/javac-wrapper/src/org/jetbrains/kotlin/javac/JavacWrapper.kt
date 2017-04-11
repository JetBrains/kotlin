/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.javac

import com.intellij.ide.highlighter.JavaFileType
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.CommonClassNames
import com.intellij.psi.search.EverythingGlobalScope
import com.intellij.psi.search.GlobalSearchScope
import com.sun.source.tree.CompilationUnitTree
import com.sun.source.util.TreePath
import com.sun.tools.javac.api.JavacTrees
import com.sun.tools.javac.code.Flags
import com.sun.tools.javac.code.Symbol
import com.sun.tools.javac.code.Symtab
import com.sun.tools.javac.file.JavacFileManager
import com.sun.tools.javac.jvm.ClassReader
import com.sun.tools.javac.main.JavaCompiler
import com.sun.tools.javac.model.JavacElements
import com.sun.tools.javac.model.JavacTypes
import com.sun.tools.javac.tree.JCTree
import com.sun.tools.javac.util.Context
import com.sun.tools.javac.util.Names
import com.sun.tools.javac.util.Options
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageLocation
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import com.sun.tools.javac.util.List as JavacList
import org.jetbrains.kotlin.wrappers.symbols.JavacClass
import org.jetbrains.kotlin.wrappers.symbols.JavacPackage
import org.jetbrains.kotlin.load.java.structure.JavaClass
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.isSubpackageOf
import org.jetbrains.kotlin.name.parentOrNull
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.wrappers.trees.JCClass
import org.jetbrains.kotlin.wrappers.trees.JCPackage
import java.io.Closeable
import java.io.File
import javax.lang.model.element.Element
import javax.lang.model.element.TypeElement
import javax.lang.model.type.TypeMirror
import javax.tools.JavaFileManager
import javax.tools.JavaFileObject
import javax.tools.StandardLocation

class JavacWrapper(javaFiles: Collection<File>,
                   kotlinFiles: Collection<KtFile>,
                   classPathRoots: List<File>,
                   outDir: File?,
                   private val messageCollector: MessageCollector?,
                   arguments: Array<String>?) : Closeable {

    companion object {
        fun getInstance(project: Project): JavacWrapper = ServiceManager.getService(project, JavacWrapper::class.java)
    }

    val JAVA_LANG_OBJECT by lazy { findClassInSymbols(CommonClassNames.JAVA_LANG_OBJECT) }

    private val context = Context()

    init {
        messageCollector?.let { JavacLogger.preRegister(context, it) }
        arguments?.toList()?.let { JavacOptionsMapper.map(Options.instance(context), it) }
    }

    private val javac = object : JavaCompiler(context) {
        override fun parseFiles(files: Iterable<JavaFileObject>?) = compilationUnits
    }
    private val fileManager = context[JavaFileManager::class.java] as JavacFileManager

    init {
        fileManager.setLocation(StandardLocation.CLASS_PATH, classPathRoots)
        outDir?.let {
            it.mkdirs()
            fileManager.setLocation(StandardLocation.CLASS_OUTPUT, listOf(it))
        }
    }

    private val symbols = Symtab.instance(context)
    private val trees = JavacTrees.instance(context)
    private val elements = JavacElements.instance(context)
    private val types = JavacTypes.instance(context)
    private val fileObjects = fileManager.getJavaFileObjectsFromFiles(javaFiles).toJavacList()
    private val compilationUnits: JavacList<JCTree.JCCompilationUnit> = fileObjects.map(javac::parse).toJavacList()

    private val javaClasses = compilationUnits
            .flatMap { unit -> unit.typeDecls
                    .flatMap { JCClass(it as JCTree.JCClassDecl, trees.getPath(unit, it), this).withInnerClasses() }
            }
            .associateBy(JavaClass::fqName)

    private val javaPackages = compilationUnits
            .map { JCPackage(it.packageName.toString(), this) }
            .associateBy(JCPackage::fqName)

    private val kotlinClassifiersCache = KotlinClassifiersCache(kotlinFiles)

    fun compile() = with(javac) {
        if (errorCount() > 0) return false

        fileManager.setClassPathForCompilation()
        messageCollector?.report(CompilerMessageSeverity.INFO,
                                 "Compiling Java sources",
                                 CompilerMessageLocation.NO_LOCATION)
        compile(fileObjects)
        errorCount() == 0
    }

    override fun close() {
        fileManager.close()
        javac.close()
    }

    fun findClass(fqName: FqName, scope: GlobalSearchScope = EverythingGlobalScope()) = when {
        scope is EverythingGlobalScope -> javaClasses[fqName] ?: findClassInSymbols(fqName.asString())
        scope.contains(AnyJavaSourceVirtualFile) -> javaClasses[fqName]
        else -> findClassInSymbols(fqName.asString()) ?: javaClasses[fqName]
    }

    fun findPackage(fqName: FqName, scope: GlobalSearchScope) = when {
        scope is EverythingGlobalScope -> javaPackages[fqName] ?: findPackageInSymbols(fqName.asString())
        scope.contains(AnyJavaSourceVirtualFile) -> javaPackages[fqName]
        else -> findPackageInSymbols(fqName.asString()) ?: javaPackages[fqName]
    }

    fun findSubPackages(fqName: FqName) = symbols.packages
                                                  .filterKeys { it.toString().startsWith("$fqName.") }
                                                  .map { JavacPackage(it.value, this) } +
                                          javaPackages
                                                  .filterKeys { it.isSubpackageOf(fqName) && it != fqName }
                                                  .map { it.value }

    fun findClassesFromPackage(fqName: FqName) = javaClasses
                                                         .filterKeys { it?.parentOrNull() == fqName }
                                                         .flatMap { it.value.withInnerClasses() } +
                                                 elements.getPackageElement(fqName.asString())
                                                         ?.members()
                                                         ?.elements
                                                         ?.filterIsInstance(TypeElement::class.java)
                                                         ?.map { JavacClass(it, this) }
                                                         .orEmpty()

    fun getTreePath(tree: JCTree, compilationUnit: CompilationUnitTree): TreePath = trees.getPath(compilationUnit, tree)

    fun getKotlinClassifier(fqName: FqName) = kotlinClassifiersCache.getKotlinClassifier(fqName)

    fun isDeprecated(element: Element) = elements.isDeprecated(element)

    fun isDeprecated(typeMirror: TypeMirror) = isDeprecated(types.asElement(typeMirror))

    private inline fun <reified T> Iterable<T>.toJavacList() = JavacList.from(this)

    private fun findClassInSymbols(fqName: String) = elements.getTypeElement(fqName)?.let { JavacClass(it, this) }

    private fun findPackageInSymbols(fqName: String) = elements.getPackageElement(fqName)?.let { JavacPackage(it, this) }

    private fun JavacFileManager.setClassPathForCompilation() = apply {
        setLocation(StandardLocation.CLASS_PATH,
                    getLocation(StandardLocation.CLASS_PATH) + getLocation(StandardLocation.CLASS_OUTPUT))

        val reader = ClassReader.instance(context)
        val names = Names.instance(context)
        val outDirName = getLocation(StandardLocation.CLASS_OUTPUT).firstOrNull()?.path ?: ""

        list(StandardLocation.CLASS_OUTPUT, "", setOf(JavaFileObject.Kind.CLASS), true)
                .forEach {
                    val fqName = it.name
                            .substringAfter(outDirName)
                            .substringBefore(".class")
                            .replace("/", ".")
                            .let { if (it.startsWith(".")) it.substring(1) else it }
                            .let(names::fromString)

                    symbols.classes[fqName]?.let { symbols.classes[fqName] = null }
                    val symbol = reader.enterClass(fqName, it)

                    (elements.getPackageOf(symbol) as? Symbol.PackageSymbol)?.let {
                        it.members_field.enter(symbol)
                        it.flags_field = it.flags_field or Flags.EXISTS.toLong()
                    }
                }

    }

    private fun JavaClass.withInnerClasses(): List<JavaClass> = listOf(this) + innerClasses.flatMap { it.withInnerClasses() }

}

private object AnyJavaSourceVirtualFile : VirtualFile() {
    override fun refresh(asynchronous: Boolean, recursive: Boolean, postRunnable: Runnable?) {}

    override fun getLength() = 0L

    override fun getFileSystem() = throw UnsupportedOperationException("Should never be called")

    override fun getPath() = ""

    override fun isDirectory() = false

    override fun getTimeStamp() = 0L

    override fun getName() = ""

    override fun contentsToByteArray() = throw UnsupportedOperationException("Should never be called")

    override fun isValid() = true

    override fun getInputStream() = throw UnsupportedOperationException("Should never be called")

    override fun getParent() = throw UnsupportedOperationException("Should never be called")

    override fun getChildren() = emptyArray<VirtualFile>()

    override fun isWritable() = false

    override fun getOutputStream(requestor: Any?, newModificationStamp: Long, newTimeStamp: Long) = throw UnsupportedOperationException("Should never be called")

    override fun getExtension() = "java"

    override fun getFileType(): FileType = JavaFileType.INSTANCE

    override fun toString() = "Java Source"
}