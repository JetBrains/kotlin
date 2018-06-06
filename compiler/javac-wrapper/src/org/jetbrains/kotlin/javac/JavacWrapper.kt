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
import com.intellij.psi.search.EverythingGlobalScope
import com.intellij.psi.search.GlobalSearchScope
import com.sun.source.tree.CompilationUnitTree
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
import org.jetbrains.kotlin.javac.resolve.ClassifierResolver
import org.jetbrains.kotlin.javac.resolve.IdentifierResolver
import org.jetbrains.kotlin.javac.resolve.KotlinClassifiersCache
import org.jetbrains.kotlin.javac.resolve.classId
import org.jetbrains.kotlin.javac.wrappers.symbols.SymbolBasedClass
import org.jetbrains.kotlin.javac.wrappers.symbols.SymbolBasedClassifierType
import org.jetbrains.kotlin.javac.wrappers.symbols.SymbolBasedPackage
import org.jetbrains.kotlin.javac.wrappers.trees.*
import org.jetbrains.kotlin.load.java.structure.*
import org.jetbrains.kotlin.name.*
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
        bootClasspath: List<File>?,
        sourcePath: List<File>?,
        val kotlinResolver: JavacWrapperKotlinResolver,
        private val compileJava: Boolean,
        private val outputDirectory: File?,
        private val context: Context
) : Closeable {
    private val localFileSystem = VirtualFileManager.getInstance().getFileSystem(StandardFileSystems.FILE_PROTOCOL)!!
    private val jarFileSystem = VirtualFileManager.getInstance().getFileSystem(StandardFileSystems.JAR_PROTOCOL)!!

    companion object {
        fun getInstance(project: Project): JavacWrapper = ServiceManager.getService(project, JavacWrapper::class.java)
    }

    private fun createCommonClassifierType(classId: ClassId) =
            findClassInSymbols(classId)?.let {
                SymbolBasedClassifierType(it.element.asType(), this)
            }

    val JAVA_LANG_OBJECT by lazy {
        createCommonClassifierType(classId("java.lang", "Object"))
    }

    val JAVA_LANG_ENUM by lazy {
        findClassInSymbols(classId("java.lang", "Enum"))
    }

    val JAVA_LANG_ANNOTATION_ANNOTATION by lazy {
        createCommonClassifierType(classId("java.lang.annotation", "Annotation"))
    }

    init {
        Options.instance(context).let { options ->
            JavacOptionsMapper.setUTF8Encoding(options)
            arguments?.toList()?.let { JavacOptionsMapper.map(options, it) }
        }
    }

    private val javac = object : JavaCompiler(context) {
        override fun parseFiles(files: Iterable<JavaFileObject>?) = compilationUnits
    }

    private val fileManager = context[JavaFileManager::class.java] as JavacFileManager

    init {
        // keep javadoc comments
        javac.keepComments = true
        // use rt.jar instead of lib/ct.sym
        fileManager.setSymbolFileEnabled(false)
        bootClasspath?.let {
            val cp = fileManager.getLocation(StandardLocation.PLATFORM_CLASS_PATH) + jvmClasspathRoots
            fileManager.setLocation(StandardLocation.PLATFORM_CLASS_PATH, it)
            fileManager.setLocation(StandardLocation.CLASS_PATH, cp)
        } ?: fileManager.setLocation(StandardLocation.CLASS_PATH, jvmClasspathRoots)
        sourcePath?.let {
            fileManager.setLocation(StandardLocation.SOURCE_PATH, sourcePath)
        }
    }

    private val names = Names.instance(context)
    private val symbols = Symtab.instance(context)
    private val elements = JavacElements.instance(context)
    private val types = JavacTypes.instance(context)
    private val fileObjects = fileManager.getJavaFileObjectsFromFiles(javaFiles).toJavacList()
    private val compilationUnits: JavacList<JCTree.JCCompilationUnit> = fileObjects.map(javac::parse).toJavacList()

    private val treeBasedJavaClasses = compilationUnits.flatMap { unit ->
        unit.typeDecls.map { classDeclaration ->
            val packageName = unit.packageName?.toString() ?: ""
            val className = (classDeclaration as JCTree.JCClassDecl).simpleName.toString()
            val classId = classId(packageName, className)
            classId to TreeBasedClass(classDeclaration, unit, this, classId, null)
        }
    }.toMap()

    private val javaPackages = compilationUnits
            .mapTo(hashSetOf<TreeBasedPackage>()) { unit ->
                unit.packageName?.toString()?.let { packageName ->
                    TreeBasedPackage(packageName, this, unit)
                } ?: TreeBasedPackage("<root>", this, unit)
            }
            .associateBy(TreeBasedPackage::fqName)

    private val packageSourceAnnotations = compilationUnits
            .filter {
                it.sourceFile.isNameCompatible("package-info", JavaFileObject.Kind.SOURCE) &&
                it.packageName != null
            }.associateBy({ FqName(it.packageName!!.toString()) }) { compilationUnit ->
        compilationUnit.packageAnnotations
    }

    private val classifierResolver = ClassifierResolver(this)
    private val identifierResolver = IdentifierResolver(this)
    private val kotlinClassifiersCache by lazy { KotlinClassifiersCache(if (javaFiles.isNotEmpty()) kotlinFiles else emptyList(), this) }
    private val symbolBasedPackagesCache = hashMapOf<String, SymbolBasedPackage?>()
    private val symbolBasedClassesCache = hashMapOf<ClassId, SymbolBasedClass>()

    fun compile(outDir: File? = null): Boolean = with(javac) {
        if (!compileJava) return true
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

    fun findClass(classId: ClassId, scope: GlobalSearchScope = EverythingGlobalScope()): JavaClass? {
        if (classId.isNestedClass) {
            val pathSegments = classId.relativeClassName.pathSegments().map { it.asString() }
            val outerClassId = ClassId(classId.packageFqName, Name.identifier(pathSegments.first()))
            var outerClass = findClass(outerClassId, scope) ?: return null

            pathSegments.drop(1).forEach {
                outerClass = outerClass.findInnerClass(Name.identifier(it)) ?: return null
            }

            return outerClass
        }

        treeBasedJavaClasses[classId]?.let { javaClass ->
            javaClass.virtualFile?.let { if (it in scope) return javaClass }
        }

        if (symbolBasedClassesCache.containsKey(classId)) {
            val javaClass = symbolBasedClassesCache[classId]
            javaClass?.virtualFile?.let { file ->
                if (file in scope) return javaClass
            }
        }

        findPackageInSymbols(classId.packageFqName.asString())?.let {
            (it.element as Symbol.PackageSymbol).findClass(classId)?.let { javaClass ->
                javaClass.virtualFile?.let { file ->
                    if (file in scope) return javaClass
                }
            }

        }

        return null
    }

    fun findPackage(fqName: FqName, scope: GlobalSearchScope = EverythingGlobalScope()): JavaPackage? {
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

    fun getPackageAnnotationsFromSources(fqName: FqName): List<JCTree.JCAnnotation> =
            packageSourceAnnotations[fqName] ?: emptyList()

    fun findClassesFromPackage(fqName: FqName): List<JavaClass> =
            treeBasedJavaClasses
                    .filterKeys { it.packageFqName == fqName }
                    .map { treeBasedJavaClasses[it.key]!! } +
            elements.getPackageElement(fqName.asString())
                    ?.members()
                    ?.elements
                    ?.filterIsInstance(Symbol.ClassSymbol::class.java)
                    ?.map { SymbolBasedClass(it, this, null, it.classfile) }
                    .orEmpty()

    fun knownClassNamesInPackage(fqName: FqName): Set<String> =
            treeBasedJavaClasses
                    .filterKeys { it.packageFqName == fqName }
                    .mapTo(hashSetOf()) { it.value.name.asString() } +
            elements.getPackageElement(fqName.asString())
                    ?.members_field
                    ?.elements
                    ?.filterIsInstance<Symbol.ClassSymbol>()
                    ?.map { it.name.toString() }
                    .orEmpty()

    fun getKotlinClassifier(classId: ClassId): JavaClass? =
            kotlinClassifiersCache.getKotlinClassifier(classId)

    fun isDeprecated(element: Element) = elements.isDeprecated(element)

    fun isDeprecated(typeMirror: TypeMirror) = isDeprecated(types.asElement(typeMirror))

    fun resolve(tree: JCTree, compilationUnit: CompilationUnitTree, containingElement: JavaElement): JavaClassifier? =
            classifierResolver.resolve(tree, compilationUnit, containingElement)

    fun resolveField(tree: JCTree, compilationUnit: CompilationUnitTree, containingClass: JavaClass): JavaField? =
            identifierResolver.resolve(tree, compilationUnit, containingClass)

    fun toVirtualFile(javaFileObject: JavaFileObject): VirtualFile? =
            javaFileObject.toUri().let { uri ->
                if (uri.scheme == "jar") {
                    jarFileSystem.findFileByPath(uri.schemeSpecificPart.substring("file:".length))
                }
                else {
                    localFileSystem.findFileByPath(uri.schemeSpecificPart)
                }
            }

    fun hasKotlinPackage(fqName: FqName) =
            if (kotlinClassifiersCache.hasPackage(fqName)) {
                fqName
            }
            else {
                null
            }

    fun isDeprecatedInJavaDoc(tree: JCTree, compilationUnit: CompilationUnitTree) =
            (compilationUnit as JCTree.JCCompilationUnit).docComments?.getCommentTree(tree)?.comment?.isDeprecated == true

    private inline fun <reified T> Iterable<T>.toJavacList() = JavacList.from(this)

    private fun findClassInSymbols(classId: ClassId): SymbolBasedClass? =
            elements.getTypeElement(classId.asSingleFqName().asString())?.let { symbol ->
                SymbolBasedClass(symbol, this, classId, symbol.classfile)
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

    private fun Symbol.PackageSymbol.findClass(classId: ClassId): SymbolBasedClass? {
        val name = classId.relativeClassName.asString()
        val nameParts = name.replace("$", ".").split(".")
        var symbol = members_field?.getElementsByName(names.fromString(nameParts.first()))
                             ?.firstOrNull() as? Symbol.ClassSymbol ?: return null
        if (nameParts.size > 1) {
            symbol.complete()
            for (it in nameParts.drop(1)) {
                symbol = symbol.members_field?.getElementsByName(names.fromString(it))?.firstOrNull() as? Symbol.ClassSymbol ?: return null
                symbol.complete()
            }
        }

        return symbol.let { SymbolBasedClass(it, this@JavacWrapper, classId, it.classfile) }
                .apply { symbolBasedClassesCache[classId] = this }
    }

}