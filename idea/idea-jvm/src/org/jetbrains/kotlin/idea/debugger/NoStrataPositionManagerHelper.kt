/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.idea.debugger

import com.intellij.debugger.SourcePosition
import com.intellij.debugger.engine.DebugProcess
import com.intellij.debugger.jdi.VirtualMachineProxyImpl
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.compiler.CompilerPaths
import com.intellij.openapi.compiler.ex.CompilerPathsEx
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.util.containers.ConcurrentWeakFactoryMap
import com.intellij.util.containers.ContainerUtil
import com.sun.jdi.Location
import com.sun.jdi.ReferenceType
import org.jetbrains.kotlin.codegen.inline.API
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.debugger.evaluate.KotlinDebuggerCaches
import org.jetbrains.kotlin.idea.refactoring.getLineCount
import org.jetbrains.kotlin.idea.refactoring.getLineStartOffset
import org.jetbrains.kotlin.idea.refactoring.toPsiFile
import org.jetbrains.kotlin.idea.util.ProjectRootsUtil
import org.jetbrains.kotlin.idea.util.application.runReadAction
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.load.kotlin.VirtualFileFinder
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.tail
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.parents
import org.jetbrains.kotlin.resolve.inline.InlineUtil
import org.jetbrains.kotlin.resolve.jvm.JvmClassName
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull
import org.jetbrains.kotlin.utils.getOrPutNullable
import org.jetbrains.org.objectweb.asm.*
import java.io.File
import java.util.*
import java.util.concurrent.ConcurrentMap

fun isInlineFunctionLineNumber(file: VirtualFile, lineNumber: Int, project: Project): Boolean {
    if (ProjectRootsUtil.isProjectSourceFile(project, file)) {
        val linesInFile = file.toPsiFile(project)?.getLineCount() ?: return false
        return lineNumber > linesInFile
    }

    return true
}

fun readBytecodeInfo(project: Project,
                     jvmName: JvmClassName,
                     file: VirtualFile): BytecodeDebugInfo? {
    return KotlinDebuggerCaches.getOrReadDebugInfoFromBytecode(project, jvmName, file)
}

fun ktLocationInfo(location: Location, isDexDebug: Boolean, project: Project,
                   preferInlined: Boolean = false, locationFile: KtFile? = null): Pair<Int, KtFile?> {
    if (isDexDebug && (locationFile == null || location.lineNumber() > locationFile.getLineCount())) {
        if (!preferInlined) {
            val thisFunLine = runReadAction { getLastLineNumberForLocation(location, project) }
            if (thisFunLine != null && thisFunLine != location.lineNumber()) {
                return thisFunLine to locationFile
            }
        }

        val inlinePosition = runReadAction { getOriginalPositionOfInlinedLine(location, project) }
        if (inlinePosition != null) {
            val (file, line) = inlinePosition
            return line + 1 to file
        }
    }

    return location.lineNumber() to locationFile
}

/**
 * Only the first line number is stored for instruction in dex. It can be obtained through location.lineNumber().
 * This method allows to get last stored linenumber for instruction.
 */
fun getLastLineNumberForLocation(location: Location, project: Project, searchScope: GlobalSearchScope = GlobalSearchScope.allScope(project)): Int? {
    val lineNumber = location.lineNumber()
    val fqName = FqName(location.declaringType().name())
    val fileName = location.sourceName()

    val method = location.method() ?: return null
    val name = method.name() ?: return null
    val signature = method.signature() ?: return null

    val debugInfo = findAndReadClassFile(fqName, fileName, project, searchScope, { isInlineFunctionLineNumber(it, lineNumber, project) }) ?: return null

    val lineMapping = debugInfo.lineTableMapping[BytecodeMethodKey(name, signature)] ?: return null
    return lineMapping.values.firstOrNull { it.contains(lineNumber) }?.last()
}

class WeakBytecodeDebugInfoStorage : ConcurrentWeakFactoryMap<BinaryCacheKey, BytecodeDebugInfo?>() {
    override fun create(key: BinaryCacheKey): BytecodeDebugInfo? {
        val bytes = readClassFileImpl(key.project, key.jvmName, key.file) ?: return null

        val smapData = readDebugInfo(bytes)
        val lineNumberMapping = readLineNumberTableMapping(bytes)

        return BytecodeDebugInfo(smapData, lineNumberMapping)
    }
    override fun createMap(): ConcurrentMap<BinaryCacheKey, BytecodeDebugInfo?> {
        return ContainerUtil.createConcurrentWeakKeyWeakValueMap()
    }
}

class BytecodeDebugInfo(val smapData: SmapData?, val lineTableMapping: Map<BytecodeMethodKey, Map<String, Set<Int>>>)

data class BytecodeMethodKey(val methodName: String, val signature: String)

data class BinaryCacheKey(val project: Project, val jvmName: JvmClassName, val file: VirtualFile)

private fun readClassFileImpl(project: Project,
                              jvmName: JvmClassName,
                              file: VirtualFile): ByteArray? {
    val fqNameWithInners = jvmName.fqNameForClassNameWithoutDollars.tail(jvmName.packageFqName)

    fun readFromLibrary(): ByteArray? {
        if (!ProjectRootsUtil.isLibrarySourceFile(project, file)) return null

        val classId = ClassId(jvmName.packageFqName, Name.identifier(fqNameWithInners.asString()))

        val fileFinder = VirtualFileFinder.getInstance(project)
        val classFile = fileFinder.findVirtualFileWithHeader(classId) ?: return null
        return classFile.contentsToByteArray(false)
    }

    fun readFromOutput(isForTestClasses: Boolean): ByteArray? {
        if (!ProjectRootsUtil.isProjectSourceFile(project, file)) return null

        val module = ProjectFileIndex.SERVICE.getInstance(project).getModuleForFile(file) ?: return null

        val outputPaths = CompilerPathsEx.getOutputPaths(arrayOf(module)).toList()
        val className = fqNameWithInners.asString().replace('.', '$')
        var classFile = findClassFileByPaths(jvmName.packageFqName.asString(), className, outputPaths)

        if (classFile == null) {
            if (!isForTestClasses) {
                return null
            }

            val outputDir = CompilerPaths.getModuleOutputDirectory(module, /*forTests = */ isForTestClasses) ?: return null

            val outputModeDirName = outputDir.name
            // FIXME: It looks like this doesn't work anymore after Kotlin gradle plugin have stopped generating Kotlin classes in java output dir
            // Originally this code did mapping like 'path/classes/test/debug' -> 'path/classes/androidTest/debug'
            val androidTestOutputDir = outputDir.parent?.parent?.findChild("androidTest")?.findChild(outputModeDirName) ?: return null

            classFile = findClassFileByPath(jvmName.packageFqName.asString(), className, androidTestOutputDir.path) ?: return null
        }

        return classFile.readBytes()
    }

    fun readFromSourceOutput(): ByteArray? = readFromOutput(false)

    fun readFromTestOutput(): ByteArray? = readFromOutput(true)

    return readFromLibrary() ?:
           readFromSourceOutput() ?:
           readFromTestOutput()
}

private fun findClassFileByPaths(packageName: String, className: String, paths: List<String>): File? =
        paths.mapNotNull { path -> findClassFileByPath(packageName, className, path) }.maxBy { it.lastModified() }

private fun findClassFileByPath(packageName: String, className: String, outputDirPath: String): File? {
    val outDirFile = File(outputDirPath).takeIf(File::exists) ?: return null

    val parentDirectory = File(outDirFile, packageName.replace(".", File.separator))
    if (!parentDirectory.exists()) return null

    if (ApplicationManager.getApplication().isUnitTestMode) {
        val beforeDexFileClassFile = File(parentDirectory, className + ".class.before_dex")
        if (beforeDexFileClassFile.exists()) {
            return beforeDexFileClassFile
        }
    }

    val classFile = File(parentDirectory, className + ".class")
    if (classFile.exists()) {
        return classFile
    }

    return null
}

private fun readLineNumberTableMapping(bytes: ByteArray): Map<BytecodeMethodKey, Map<String, Set<Int>>> {
    val lineNumberMapping = HashMap<BytecodeMethodKey, Map<String, Set<Int>>>()

    ClassReader(bytes).accept(object : ClassVisitor(API) {
        override fun visitMethod(access: Int, name: String?, desc: String?, signature: String?, exceptions: Array<out String>?): MethodVisitor? {
            if (name == null || desc == null) {
                return null
            }

            val methodKey = BytecodeMethodKey(name, desc)
            val methodLinesMapping = HashMap<String, MutableSet<Int>>()
            lineNumberMapping[methodKey] = methodLinesMapping

            return object : MethodVisitor(Opcodes.ASM5, null) {
                override fun visitLineNumber(line: Int, start: Label?) {
                    if (start != null) {
                        methodLinesMapping.getOrPutNullable(start.toString(), { LinkedHashSet<Int>() }).add(line)
                    }
                }
            }
        }
    }, ClassReader.SKIP_FRAMES and ClassReader.SKIP_CODE)

    return lineNumberMapping
}

internal fun getOriginalPositionOfInlinedLine(location: Location, project: Project): Pair<KtFile, Int>? {
    val lineNumber = location.lineNumber()
    val fqName = FqName(location.declaringType().name())
    val fileName = location.sourceName()
    val searchScope = GlobalSearchScope.allScope(project)

    val debugInfo = findAndReadClassFile(fqName, fileName, project, searchScope, { isInlineFunctionLineNumber(it, lineNumber, project) }) ?:
                    return null
    val smapData = debugInfo.smapData ?: return null

    return mapStacktraceLineToSource(smapData, lineNumber, project, SourceLineKind.EXECUTED_LINE, searchScope)
}

private fun findAndReadClassFile(
        fqName: FqName, fileName: String, project: Project, searchScope: GlobalSearchScope,
        fileFilter: (VirtualFile) -> Boolean): BytecodeDebugInfo? {
    val internalName = fqName.asString().replace('.', '/')
    val jvmClassName = JvmClassName.byInternalName(internalName)

    val file = DebuggerUtils.findSourceFileForClassIncludeLibrarySources(project, searchScope, jvmClassName, fileName) ?: return null

    val virtualFile = file.virtualFile ?: return null
    if (!fileFilter(virtualFile)) return null

    return readBytecodeInfo(project, jvmClassName, virtualFile)
}

internal fun getLocationsOfInlinedLine(type: ReferenceType, position: SourcePosition, sourceSearchScope: GlobalSearchScope): List<Location> {
    val line = position.line
    val file = position.file
    val project = position.file.project

    val lineStartOffset = file.getLineStartOffset(line) ?: return listOf()
    val element = file.findElementAt(lineStartOffset) ?: return listOf()
    val ktElement = element.parents.firstIsInstanceOrNull<KtElement>() ?: return listOf()

    val isInInline = runReadAction { element.parents.any { it is KtFunction && it.hasModifier(KtTokens.INLINE_KEYWORD) } }

    if (!isInInline) {
        // Lambdas passed to crossinline arguments are inlined when they are used in non-inlined lambdas
        val isInCrossinlineArgument = isInCrossinlineArgument(ktElement)
        if (!isInCrossinlineArgument) {
            return listOf()
        }
    }

    val lines = inlinedLinesNumbers(line + 1, position.file.name, FqName(type.name()), type.sourceName(), project, sourceSearchScope)

    return lines.flatMap { type.locationsOfLine(it) }
}

fun isInCrossinlineArgument(ktElement: KtElement): Boolean {
    val argumentFunctions = runReadAction {
        ktElement.parents.filter {
            when (it) {
                is KtFunctionLiteral -> it.parent is KtLambdaExpression && (it.parent.parent is KtValueArgument || it.parent.parent is KtLambdaArgument)
                is KtFunction -> it.parent is KtValueArgument
                else -> false
            }
        }.filterIsInstance<KtFunction>()
    }

    val bindingContext = ktElement.analyze(BodyResolveMode.PARTIAL)
    return argumentFunctions.any {
        val argumentDescriptor = InlineUtil.getInlineArgumentDescriptor(it, bindingContext)
        argumentDescriptor?.isCrossinline ?: false
    }
}


private fun inlinedLinesNumbers(
        inlineLineNumber: Int, inlineFileName: String,
        destinationTypeFqName: FqName, destinationFileName: String,
        project: Project, sourceSearchScope: GlobalSearchScope): List<Int> {
    val internalName = destinationTypeFqName.asString().replace('.', '/')
    val jvmClassName = JvmClassName.byInternalName(internalName)

    val file = DebuggerUtils.findSourceFileForClassIncludeLibrarySources(project, sourceSearchScope, jvmClassName, destinationFileName) ?:
               return listOf()

    val virtualFile = file.virtualFile ?: return listOf()

    val debugInfo = readBytecodeInfo(project, jvmClassName, virtualFile) ?: return listOf()
    val smapData = debugInfo.smapData ?: return listOf()

    val smap = smapData.kotlinStrata ?: return listOf()

    val mappingsToInlinedFile = smap.fileMappings.filter { it.name == inlineFileName }
    val mappingIntervals = mappingsToInlinedFile.flatMap { it.lineMappings }

    return mappingIntervals.asSequence().
            filter { rangeMapping -> rangeMapping.hasMappingForSource(inlineLineNumber) }.
            map { rangeMapping -> rangeMapping.mapSourceToDest(inlineLineNumber) }.
            filter { line -> line != -1 }.
            toList()
}

@Volatile var emulateDexDebugInTests: Boolean = false

fun DebugProcess.isDexDebug() =
        (emulateDexDebugInTests && ApplicationManager.getApplication().isUnitTestMode) ||
        (this.virtualMachineProxy as? VirtualMachineProxyImpl)?.virtualMachine?.name() == "Dalvik" // TODO: check other machine names