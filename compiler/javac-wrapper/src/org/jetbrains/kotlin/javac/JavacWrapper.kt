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
import com.intellij.openapi.vfs.StandardFileSystems
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
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
import com.sun.tools.javac.util.Log
import com.sun.tools.javac.util.Names
import com.sun.tools.javac.util.Options
import org.jetbrains.kotlin.javac.wrappers.symbols.SymbolBasedClass
import org.jetbrains.kotlin.javac.wrappers.symbols.SymbolBasedClassifierType
import org.jetbrains.kotlin.javac.wrappers.symbols.SymbolBasedPackage
import org.jetbrains.kotlin.javac.wrappers.trees.*
import org.jetbrains.kotlin.load.java.structure.JavaAnnotation
import org.jetbrains.kotlin.load.java.structure.JavaClass
import org.jetbrains.kotlin.load.java.structure.JavaClassifier
import org.jetbrains.kotlin.load.java.structure.JavaPackage
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.isSubpackageOf
import org.jetbrains.kotlin.name.parentOrNull
import org.jetbrains.kotlin.psi.KtFile
import java.io.Closeable
import java.io.File
import javax.lang.model.element.Element
import javax.lang.model.type.TypeMirror
import javax.tools.JavaFileManager
import javax.tools.JavaFileObject
import javax.tools.StandardLocation
import com.sun.tools.javac.util.List as JavacList

class JavacWrapper(
        javaFiles: Collection<File>,
        kotlinFiles: Collection<KtFile>,
        arguments: Array<String>?,
        jvmClasspathRoots: List<File>,
        private val outputDirectory: File?,
        private val context: Context
) : Closeable {
    private val localFileSystem = VirtualFileManager.getInstance().getFileSystem(StandardFileSystems.FILE_PROTOCOL)!!
    private val jarFileSystem = VirtualFileManager.getInstance().getFileSystem(StandardFileSystems.JAR_PROTOCOL)!!

    companion object {
        fun getInstance(project: Project): JavacWrapper = ServiceManager.getService(project, JavacWrapper::class.java)
    }

    private fun createCommonClassifierType(fqName: String) =
            findClassInSymbols(fqName)?.let {
                SymbolBasedClassifierType(it.element.asType(), this)
            }

    val JAVA_LANG_OBJECT by lazy {
        createCommonClassifierType(CommonClassNames.JAVA_LANG_OBJECT)
    }

    val JAVA_LANG_ENUM by lazy {
        createCommonClassifierType(CommonClassNames.JAVA_LANG_ENUM)
    }

    val JAVA_LANG_ANNOTATION_ANNOTATION by lazy {
        createCommonClassifierType(CommonClassNames.JAVA_LANG_ANNOTATION_ANNOTATION)
    }

    init {
        arguments?.toList()?.let { JavacOptionsMapper.map(Options.instance(context), it) }
    }

    private val javac = object : JavaCompiler(context) {
        override fun parseFiles(files: Iterable<JavaFileObject>?) = compilationUnits
    }

    private val fileManager = context[JavaFileManager::class.java] as JavacFileManager

    init {
        // use rt.jar instead of lib/ct.sym
        fileManager.setSymbolFileEnabled(false)
        fileManager.setLocation(StandardLocation.CLASS_PATH, jvmClasspathRoots)
    }

    private val names = Names.instance(context)
    private val symbols = Symtab.instance(context)
    private val trees = JavacTrees.instance(context)
    private val elements = JavacElements.instance(context)
    private val types = JavacTypes.instance(context)
    private val fileObjects = fileManager.getJavaFileObjectsFromFiles(javaFiles).toJavacList()
    private val compilationUnits: JavacList<JCTree.JCCompilationUnit> = fileObjects.map(javac::parse).toJavacList()

    private val javaClasses = compilationUnits
            .flatMap { unit ->
                unit.typeDecls.flatMap { classDecl ->
                    TreeBasedClass(classDecl as JCTree.JCClassDecl,
                                   trees.getPath(unit, classDecl),
                                   this,
                                   unit.sourceFile).withInnerClasses()
                }
            }
            .associateBy(JavaClass::fqName)

    private val javaClassesAssociatedByClassId =
            javaClasses.values.associateBy { it.computeClassId() }

    private val javaPackages = compilationUnits
            .mapNotNullTo(hashSetOf()) { unit ->
                unit.packageName?.toString()?.let { packageName ->
                    TreeBasedPackage(packageName, this, unit.sourcefile)
                }
            }
            .associateBy(TreeBasedPackage::fqName)

    private val packageSourceAnnotations = compilationUnits
            .filter {
                it.sourceFile.isNameCompatible("package-info", JavaFileObject.Kind.SOURCE) &&
                it.packageName != null
            }.associateBy({ FqName(it.packageName!!.toString()) }) { compilationUnit ->
                compilationUnit.packageAnnotations.map { TreeBasedAnnotation(it, compilationUnit, this) }
            }

    private val kotlinClassifiersCache = KotlinClassifiersCache(if (javaFiles.isNotEmpty()) kotlinFiles else emptyList(), this)
    private val treePathResolverCache = TreePathResolverCache(this)
    private val symbolBasedClassesCache = hashMapOf<String, SymbolBasedClass?>()
    private val symbolBasedPackagesCache = hashMapOf<String, SymbolBasedPackage?>()

    fun compile(outDir: File? = null): Boolean = with(javac) {
        if (errorCount() > 0) return false

        val javaFilesNumber = fileObjects.length()
        if (javaFilesNumber == 0) return true

        fileManager.setClassPathForCompilation(outDir)
        context.get(Log.outKey)?.println("Compiling $javaFilesNumber Java source files" +
                                         " to [${fileManager.getLocation(StandardLocation.CLASS_OUTPUT)?.firstOrNull()?.path}]")
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

    fun findClass(classId: ClassId, scope: GlobalSearchScope = EverythingGlobalScope()): JavaClass? {
        javaClassesAssociatedByClassId[classId]?.let { javaClass ->
            javaClass.virtualFile?.let { if (it in scope) return javaClass }
        }

        findPackageInSymbols(classId.packageFqName.asString())?.let {
            (it.element as Symbol.PackageSymbol).findClass(classId.relativeClassName.asString())?.let { javaClass ->
                javaClass.virtualFile?.let { file ->
                    if (file in scope) return javaClass
                }
            }

        }

        return null
    }

    fun findPackage(fqName: FqName, scope: GlobalSearchScope): JavaPackage? {
        javaPackages[fqName]?.let { javaPackage ->
            javaPackage.virtualFile?.let { file ->
                if (file in scope) return javaPackage
            }
        }

        return findPackageInSymbols(fqName.asString())
    }

    fun findSubPackages(fqName: FqName): List<JavaPackage> =
            symbols.packages
                    .filterKeys { it.toString().startsWith("$fqName.") }
                    .map { SymbolBasedPackage(it.value, this) } +
            javaPackages
                    .filterKeys { it.isSubpackageOf(fqName) && it != fqName }
                    .map { it.value }

    fun getPackageAnnotationsFromSources(fqName: FqName): List<JavaAnnotation> =
            packageSourceAnnotations[fqName] ?: emptyList()

    fun findClassesFromPackage(fqName: FqName): List<JavaClass> =
            javaClasses
                    .filterKeys { it?.parentOrNull() == fqName }
                    .flatMap { it.value.withInnerClasses() } +
            elements.getPackageElement(fqName.asString())
                    ?.members()
                    ?.elements
                    ?.filterIsInstance(Symbol.ClassSymbol::class.java)
                    ?.map { SymbolBasedClass(it, this, it.classfile) }
                    .orEmpty()

    fun knownClassNamesInPackage(fqName: FqName): Set<String> =
            javaClasses.filterKeys { it?.parentOrNull() == fqName }
                    .mapTo(hashSetOf()) { it.value.name.asString() } +
            elements.getPackageElement(fqName.asString())
                    ?.members_field
                    ?.elements
                    ?.filterIsInstance<Symbol.ClassSymbol>()
                    ?.map { it.name.toString() }
                    .orEmpty()

    fun getTreePath(tree: JCTree, compilationUnit: CompilationUnitTree): TreePath =
            trees.getPath(compilationUnit, tree)

    fun getKotlinClassifier(fqName: FqName): JavaClass? =
            kotlinClassifiersCache.getKotlinClassifier(fqName)

    fun isDeprecated(element: Element) = elements.isDeprecated(element)

    fun isDeprecated(typeMirror: TypeMirror) = isDeprecated(types.asElement(typeMirror))

    fun resolve(treePath: TreePath): JavaClassifier? =
            treePathResolverCache.resolve(treePath)

    fun toVirtualFile(javaFileObject: JavaFileObject): VirtualFile? =
            javaFileObject.toUri().let { uri ->
                if (uri.scheme == "jar") {
                    jarFileSystem.findFileByPath(uri.schemeSpecificPart.substring("file:".length))
                }
                else {
                    localFileSystem.findFileByPath(uri.schemeSpecificPart)
                }
            }

    private inline fun <reified T> Iterable<T>.toJavacList() = JavacList.from(this)

    private fun findClassInSymbols(fqName: String): SymbolBasedClass? {
        if (symbolBasedClassesCache.containsKey(fqName)) return symbolBasedClassesCache[fqName]

        elements.getTypeElement(fqName)?.let { symbol ->
            SymbolBasedClass(symbol, this, symbol.classfile)
        }.let { symbolBasedClass ->
            symbolBasedClassesCache[fqName] = symbolBasedClass
            return symbolBasedClass
        }
    }

    private fun findPackageInSymbols(fqName: String): SymbolBasedPackage? {
        if (symbolBasedPackagesCache.containsKey(fqName)) return symbolBasedPackagesCache[fqName]

        elements.getPackageElement(fqName)?.let { symbol ->
            SymbolBasedPackage(symbol, this)
        }.let { symbolBasedPackage ->
            symbolBasedPackagesCache[fqName] = symbolBasedPackage
            return symbolBasedPackage
        }
    }

    private fun JavacFileManager.setClassPathForCompilation(outDir: File?) = apply {
        (outDir ?: outputDirectory)?.let { outputDir ->
            outputDir.mkdirs()
            fileManager.setLocation(StandardLocation.CLASS_OUTPUT, listOf(outputDir))
        }

        val reader = ClassReader.instance(context)
        val names = Names.instance(context)
        val outDirName = getLocation(StandardLocation.CLASS_OUTPUT)?.firstOrNull()?.path ?: ""

        list(StandardLocation.CLASS_OUTPUT, "", setOf(JavaFileObject.Kind.CLASS), true)
                .forEach { fileObject ->
                    val fqName = fileObject.name
                            .substringAfter(outDirName)
                            .substringBefore(".class")
                            .replace(File.separator, ".")
                            .let { className ->
                                if (className.startsWith(".")) className.substring(1) else className
                            }.let(names::fromString)

                    symbols.classes[fqName]?.let { symbols.classes[fqName] = null }
                    val symbol = reader.enterClass(fqName, fileObject)

                    (elements.getPackageOf(symbol) as? Symbol.PackageSymbol)?.let { packageSymbol ->
                        packageSymbol.members_field.enter(symbol)
                        packageSymbol.flags_field = packageSymbol.flags_field or Flags.EXISTS.toLong()
                    }
                }

    }

    private fun TreeBasedClass.withInnerClasses(): List<TreeBasedClass> =
            listOf(this) + innerClasses.values.flatMap { it.withInnerClasses() }

    private fun Symbol.PackageSymbol.findClass(name: String): SymbolBasedClass? {
        val nameParts = name.replace("$", ".").split(".")
        var symbol = members_field.getElementsByName(names.fromString(nameParts.first()))
                             ?.firstOrNull() as? Symbol.ClassSymbol ?: return null
        if (nameParts.size > 1) {
            symbol.complete()
            for (it in nameParts.drop(1)) {
                symbol = symbol.members_field?.getElementsByName(names.fromString(it))?.firstOrNull() as? Symbol.ClassSymbol ?: return null
                symbol.complete()
            }
        }

        return symbol.let { SymbolBasedClass(it, this@JavacWrapper, it.classfile) }
    }

}
