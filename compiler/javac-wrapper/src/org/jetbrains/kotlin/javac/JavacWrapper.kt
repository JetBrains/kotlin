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

import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFileSystem
import com.intellij.openapi.vfs.local.CoreLocalFileSystem
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
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.JVMConfigurationKeys
import com.sun.tools.javac.util.List as JavacList
import org.jetbrains.kotlin.javac.wrappers.symbols.SymbolBasedClass
import org.jetbrains.kotlin.javac.wrappers.symbols.SymbolBasedPackage
import org.jetbrains.kotlin.load.java.structure.JavaClass
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.isSubpackageOf
import org.jetbrains.kotlin.name.parentOrNull
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.javac.wrappers.trees.TreeBasedClass
import org.jetbrains.kotlin.javac.wrappers.trees.TreeBasedPackage
import org.jetbrains.kotlin.javac.wrappers.trees.TreePathResolverCache
import org.jetbrains.kotlin.load.java.structure.JavaPackage
import java.io.Closeable
import java.io.File
import javax.lang.model.element.Element
import javax.lang.model.type.TypeMirror
import javax.tools.JavaFileManager
import javax.tools.JavaFileObject
import javax.tools.StandardLocation

class JavacWrapper(javaFiles: Collection<File>,
                   kotlinFiles: Collection<KtFile>,
                   classPathRoots: List<File>,
                   private val configuration: CompilerConfiguration,
                   private val messageCollector: MessageCollector?,
                   arguments: Array<String>?,
                   private val localFileSystem: CoreLocalFileSystem,
                   private val jarFileSystem: VirtualFileSystem) : Closeable {

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
        // use rt.jar instead of lib/ct.sym
        fileManager.setSymbolFileEnabled(false)
        fileManager.setLocation(StandardLocation.CLASS_PATH, classPathRoots)
    }

    private val symbols = Symtab.instance(context)
    private val trees = JavacTrees.instance(context)
    private val elements = JavacElements.instance(context)
    private val types = JavacTypes.instance(context)
    private val fileObjects = fileManager.getJavaFileObjectsFromFiles(javaFiles).toJavacList()
    private val compilationUnits: JavacList<JCTree.JCCompilationUnit> = fileObjects.map(javac::parse).toJavacList()

    private val javaClasses = compilationUnits
            .flatMap { unit -> unit.typeDecls
                    .flatMap { TreeBasedClass(it as JCTree.JCClassDecl,
                                              trees.getPath(unit, it),
                                              this,
                                              unit.sourceFile)
                            .withInnerClasses() }
            }
            .associateBy(JavaClass::fqName)

    private val javaPackages = compilationUnits
            .mapNotNullTo(hashSetOf()) { unit -> unit.packageName?.toString()?.let { TreeBasedPackage(it, this, unit.sourcefile) } }
            .associateBy(TreeBasedPackage::fqName)

    private val kotlinClassifiersCache = KotlinClassifiersCache(kotlinFiles, this)
    private val treePathResolverCache = TreePathResolverCache(this)

    fun compile(outDir: File? = null) = with(javac) {
        if (errorCount() > 0) return false

        fileManager.setClassPathForCompilation(outDir)
        messageCollector?.report(CompilerMessageSeverity.INFO,
                                 "Compiling Java sources")
        compile(fileObjects)
        errorCount() == 0
    }

    override fun close() {
        fileManager.close()
        javac.close()
    }

    fun findClass(fqName: FqName, scope: GlobalSearchScope = EverythingGlobalScope()): JavaClass? {
        javaClasses[fqName]?.let { javaClass ->
            javaClass.virtualFile?.let { if (it in scope) return javaClass }
        }

        findClassInSymbols(fqName.asString())?.let { javaClass ->
            javaClass.virtualFile?.let { if (it in scope) return javaClass }
        }

        return null
    }

    fun findPackage(fqName: FqName, scope: GlobalSearchScope): JavaPackage? {
        javaPackages[fqName]?.let { javaPackage ->
            javaPackage.virtualFile?.let { if (it in scope) return javaPackage }
        }

        return findPackageInSymbols(fqName.asString())
    }

    fun findSubPackages(fqName: FqName) = symbols.packages
                                                  .filterKeys { it.toString().startsWith("$fqName.") }
                                                  .map { SymbolBasedPackage(it.value, this) } +
                                          javaPackages
                                                  .filterKeys { it.isSubpackageOf(fqName) && it != fqName }
                                                  .map { it.value }

    fun findClassesFromPackage(fqName: FqName) = javaClasses
                                                         .filterKeys { it?.parentOrNull() == fqName }
                                                         .flatMap { it.value.withInnerClasses() } +
                                                 elements.getPackageElement(fqName.asString())
                                                         ?.members()
                                                         ?.elements
                                                         ?.filterIsInstance(Symbol.ClassSymbol::class.java)
                                                         ?.map { SymbolBasedClass(it, this, it.classfile) }
                                                         .orEmpty()

    fun getTreePath(tree: JCTree, compilationUnit: CompilationUnitTree): TreePath = trees.getPath(compilationUnit, tree)

    fun getKotlinClassifier(fqName: FqName) = kotlinClassifiersCache.getKotlinClassifier(fqName)

    fun isDeprecated(element: Element) = elements.isDeprecated(element)

    fun isDeprecated(typeMirror: TypeMirror) = isDeprecated(types.asElement(typeMirror))

    fun resolve(treePath: TreePath) = treePathResolverCache.resolve(treePath)

    fun toVirtualFile(javaFileObject: JavaFileObject) = javaFileObject.toUri().let {
        if (it.scheme == "jar") {
            jarFileSystem.findFileByPath(it.schemeSpecificPart.substring("file:".length))
        } else {
            localFileSystem.findFileByPath(it.schemeSpecificPart)
        }
    }

    private inline fun <reified T> Iterable<T>.toJavacList() = JavacList.from(this)

    private fun findClassInSymbols(fqName: String) = elements.getTypeElement(fqName)?.let { SymbolBasedClass(it, this, it.classfile) }

    private fun findPackageInSymbols(fqName: String) = elements.getPackageElement(fqName)?.let { SymbolBasedPackage(it, this) }

    private fun JavacFileManager.setClassPathForCompilation(outDir: File?) = apply {
        (outDir ?: configuration[JVMConfigurationKeys.OUTPUT_DIRECTORY])?.let {
            it.mkdirs()
            fileManager.setLocation(StandardLocation.CLASS_OUTPUT, listOf(it))
        }

        val reader = ClassReader.instance(context)
        val names = Names.instance(context)
        val outDirName = getLocation(StandardLocation.CLASS_OUTPUT)?.firstOrNull()?.path ?: ""

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

    private fun TreeBasedClass<JCTree.JCClassDecl>.withInnerClasses(): List<TreeBasedClass<JCTree.JCClassDecl>> = listOf(this) + innerClasses.values.flatMap { it.withInnerClasses() }

}