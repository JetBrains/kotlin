/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

import com.intellij.debugger.DebuggerInvocationUtil
import com.intellij.debugger.SourcePosition
import com.intellij.debugger.actions.MethodSmartStepTarget
import com.intellij.debugger.actions.SmartStepTarget
import com.intellij.debugger.engine.*
import com.intellij.debugger.engine.evaluation.CodeFragmentKind
import com.intellij.debugger.engine.evaluation.EvaluationContextImpl
import com.intellij.debugger.engine.evaluation.TextWithImportsImpl
import com.intellij.debugger.impl.DebuggerContextImpl
import com.intellij.debugger.impl.JvmSteppingCommandProvider
import com.intellij.debugger.impl.PositionUtil
import com.intellij.debugger.settings.DebuggerSettings
import com.intellij.debugger.ui.breakpoints.Breakpoint
import com.intellij.debugger.ui.breakpoints.BreakpointManager
import com.intellij.debugger.ui.breakpoints.LineBreakpoint
import com.intellij.execution.process.ProcessOutputTypes
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.roots.JdkOrderEntry
import com.intellij.openapi.roots.libraries.LibraryUtil
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.psi.search.FilenameIndex
import com.intellij.xdebugger.XDebuggerManager
import com.intellij.xdebugger.XDebuggerUtil
import com.intellij.xdebugger.breakpoints.*
import com.sun.jdi.request.StepRequest
import org.jetbrains.java.debugger.breakpoints.properties.JavaBreakpointProperties
import org.jetbrains.java.debugger.breakpoints.properties.JavaLineBreakpointProperties
import org.jetbrains.kotlin.idea.debugger.breakpoints.KotlinFieldBreakpoint
import org.jetbrains.kotlin.idea.debugger.breakpoints.KotlinFieldBreakpointType
import org.jetbrains.kotlin.idea.debugger.breakpoints.KotlinLineBreakpointType
import org.jetbrains.kotlin.idea.debugger.stepping.*
import org.jetbrains.kotlin.idea.util.application.runReadAction
import org.jetbrains.kotlin.idea.util.application.runWriteAction
import org.jetbrains.kotlin.psi.psiUtil.getElementTextWithContext
import org.jetbrains.kotlin.test.InTextDirectivesUtils
import org.jetbrains.kotlin.test.InTextDirectivesUtils.findStringWithPrefixes
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstance
import java.io.File
import java.lang.AssertionError
import javax.swing.SwingUtilities

abstract class KotlinDebuggerTestBase : KotlinDebuggerTestCase() {
    private var oldSettings: DebuggerSettings? = null
    private var oldIsFilterForStdlibAlreadyAdded = false
    private var oldDisableKotlinInternalClasses = false
    private var oldRenderDelegatedProperties = false

    @Volatile
    protected var _evaluationContext: EvaluationContextImpl? = null
    protected val evaluationContext get() = _evaluationContext!!

    @Volatile
    protected var _debuggerContext: DebuggerContextImpl? = null
    protected val debuggerContext get() = _debuggerContext!!

    @Volatile
    protected var _commandProvider: KotlinSteppingCommandProvider? = null
    protected val commandProvider get() = _commandProvider!!

    override fun initApplication() {
        super.initApplication()
        saveDefaultSettings()
    }

    override fun tearDown() {
        super.tearDown()

        restoreDefaultSettings()

        _evaluationContext = null
        _debuggerContext = null
        _commandProvider = null
    }

    protected fun configureSettings(fileText: String) {
        val kotlinSettings = KotlinDebuggerSettings.getInstance()
        kotlinSettings.DEBUG_IS_FILTER_FOR_STDLIB_ALREADY_ADDED = false
        kotlinSettings.DEBUG_DISABLE_KOTLIN_INTERNAL_CLASSES = fileText.getValueForSetting("DISABLE_KOTLIN_INTERNAL_CLASSES", oldDisableKotlinInternalClasses)
        kotlinSettings.DEBUG_RENDER_DELEGATED_PROPERTIES = fileText.getValueForSetting("RENDER_DELEGATED_PROPERTIES", oldRenderDelegatedProperties)

        val debuggerSettings = DebuggerSettings.getInstance()!!
        debuggerSettings.SKIP_SYNTHETIC_METHODS = fileText.getValueForSetting("SKIP_SYNTHETIC_METHODS", oldSettings!!.SKIP_SYNTHETIC_METHODS)
        debuggerSettings.SKIP_CONSTRUCTORS = fileText.getValueForSetting("SKIP_CONSTRUCTORS", oldSettings!!.SKIP_CONSTRUCTORS)
        debuggerSettings.SKIP_CLASSLOADERS = fileText.getValueForSetting("SKIP_CLASSLOADERS", oldSettings!!.SKIP_CLASSLOADERS)
        debuggerSettings.TRACING_FILTERS_ENABLED = fileText.getValueForSetting("TRACING_FILTERS_ENABLED", oldSettings!!.TRACING_FILTERS_ENABLED)
        debuggerSettings.SKIP_GETTERS = fileText.getValueForSetting("SKIP_GETTERS", oldSettings!!.SKIP_GETTERS)
    }

    private fun String.getValueForSetting(name: String, defaultValue: Boolean): Boolean {
        return findStringWithPrefixes(this, "// $name: ")?.toBoolean() ?: defaultValue
    }

    private fun saveDefaultSettings() {
        oldIsFilterForStdlibAlreadyAdded = KotlinDebuggerSettings.getInstance().DEBUG_IS_FILTER_FOR_STDLIB_ALREADY_ADDED
        oldDisableKotlinInternalClasses = KotlinDebuggerSettings.getInstance().DEBUG_DISABLE_KOTLIN_INTERNAL_CLASSES
        oldRenderDelegatedProperties = KotlinDebuggerSettings.getInstance().DEBUG_RENDER_DELEGATED_PROPERTIES
        oldSettings = DebuggerSettings.getInstance()!!.clone()
    }

    private fun restoreDefaultSettings() {
        KotlinDebuggerSettings.getInstance().DEBUG_IS_FILTER_FOR_STDLIB_ALREADY_ADDED = oldIsFilterForStdlibAlreadyAdded
        KotlinDebuggerSettings.getInstance().DEBUG_DISABLE_KOTLIN_INTERNAL_CLASSES = oldDisableKotlinInternalClasses
        KotlinDebuggerSettings.getInstance().DEBUG_RENDER_DELEGATED_PROPERTIES = oldRenderDelegatedProperties

        val debuggerSettings = DebuggerSettings.getInstance()!!
        debuggerSettings.SKIP_SYNTHETIC_METHODS = oldSettings!!.SKIP_SYNTHETIC_METHODS
        debuggerSettings.SKIP_CONSTRUCTORS = oldSettings!!.SKIP_CONSTRUCTORS
        debuggerSettings.SKIP_CLASSLOADERS = oldSettings!!.SKIP_CLASSLOADERS
        debuggerSettings.TRACING_FILTERS_ENABLED = oldSettings!!.TRACING_FILTERS_ENABLED
        debuggerSettings.SKIP_GETTERS = oldSettings!!.SKIP_GETTERS
    }

    protected val dp: DebugProcessImpl
        get() = debugProcess ?: throw AssertionError("createLocalProcess() should be called before getDebugProcess()")

    fun doOnBreakpoint(action: SuspendContextImpl.() -> Unit) {
        super.onBreakpoint(SuspendContextRunnable {
            try {
                initContexts(it)
                it.printContext()
                it.action()
            }
            catch(e: AssertionError) {
                throw e
            }
            catch(e: Throwable) {
                e.printStackTrace()
                resume(it)
            }
        })
    }

    protected fun initContexts(suspendContext: SuspendContextImpl) {
        _evaluationContext = createEvaluationContext(suspendContext)
        _debuggerContext = createDebuggerContext(suspendContext)
        _commandProvider = JvmSteppingCommandProvider.EP_NAME.extensions.firstIsInstance<KotlinSteppingCommandProvider>()
    }

    protected fun SuspendContextImpl.doStepInto(ignoreFilters: Boolean, smartStepFilter: MethodFilter?) {
        val stepIntoCommand = runReadAction {
            commandProvider.getStepIntoCommand(this, ignoreFilters, smartStepFilter, StepRequest.STEP_LINE)
        } ?: dp.createStepIntoCommand(this, ignoreFilters, smartStepFilter)
        dp.managerThread.schedule(stepIntoCommand)
    }

    protected fun SuspendContextImpl.doStepOut() {
        val stepOutCommand = runReadAction { commandProvider.getStepOutCommand(this, debuggerContext) }
                             ?: dp.createStepOutCommand(this)
        dp.managerThread.schedule(stepOutCommand)
    }

    protected fun SuspendContextImpl.doStepOver() {
        val stepOverCommand = runReadAction { commandProvider.getStepOverCommand(this, false, debuggerContext) }
                             ?: dp.createStepOverCommand(this, false)
        dp.managerThread.schedule(stepOverCommand)
    }

    protected fun doStepping(path: String) {
        val file = File(path)
        file.readLines().forEach {
            val line = it.trim()
            processSteppingInstruction(line)
        }
    }

    protected fun processSteppingInstruction(line: String) {
        fun repeat(indexPrefix: String, f: SuspendContextImpl.() -> Unit) {
            for (i in 1..(InTextDirectivesUtils.getPrefixedInt(line, indexPrefix) ?: 1)) {
                doOnBreakpoint(f)
            }
        }

        when {
            !line.startsWith("//") -> return
            line.startsWith("// STEP_INTO: ") -> repeat("// STEP_INTO: ") { doStepInto(false, null) }
            line.startsWith("// STEP_OUT: ") -> repeat("// STEP_OUT: ") { doStepOut() }
            line.startsWith("// STEP_OVER: ") -> repeat("// STEP_OVER: ") { doStepOver() }
            line.startsWith("// SMART_STEP_INTO_BY_INDEX: ") -> doOnBreakpoint { doSmartStepInto(InTextDirectivesUtils.getPrefixedInt(line, "// SMART_STEP_INTO_BY_INDEX: ")!!) }
            line.startsWith("// SMART_STEP_INTO: ") -> repeat("// SMART_STEP_INTO: ") { doSmartStepInto() }
            line.startsWith("// RESUME: ") -> repeat("// RESUME: ") { resume(this) }
        }
    }

    protected fun SuspendContextImpl.doSmartStepInto(chooseFromList: Int = 0) {
        this.doSmartStepInto(chooseFromList, false)
    }

    private fun SuspendContextImpl.doSmartStepInto(chooseFromList: Int, ignoreFilters: Boolean) {
        val filters = createSmartStepIntoFilters()
        if (chooseFromList == 0) {
            filters.forEach {
                dp.managerThread!!.schedule(dp.createStepIntoCommand(this, ignoreFilters, it))
            }
        }
        else {
            try {
                dp.managerThread!!.schedule(dp.createStepIntoCommand(this, ignoreFilters, filters[chooseFromList - 1]))
            }
            catch(e: IndexOutOfBoundsException) {
                throw AssertionError("Couldn't find smart step into command at: \n" +
                                     runReadAction { debuggerContext.sourcePosition.elementAt.getElementTextWithContext() },
                                     e)
            }
        }
    }

    private fun createSmartStepIntoFilters(): List<MethodFilter> {
        return runReadAction {
            val position = debuggerContext.sourcePosition

            val stepTargets = KotlinSmartStepIntoHandler().findSmartStepTargets(position)
            stepTargets.filterIsInstance<SmartStepTarget>().mapNotNull {
                stepTarget ->
                when (stepTarget) {
                    is KotlinLambdaSmartStepTarget ->
                        KotlinLambdaMethodFilter(
                                stepTarget.getLambda(), stepTarget.getCallingExpressionLines()!!, stepTarget.isInline, stepTarget.isSuspend)
                    is KotlinMethodSmartStepTarget ->
                        KotlinBasicStepMethodFilter(stepTarget.descriptor, stepTarget.getCallingExpressionLines()!!)
                    is MethodSmartStepTarget -> BasicStepMethodFilter(stepTarget.method, stepTarget.getCallingExpressionLines())
                    else -> null
                }
            }
        }
    }

    protected fun SuspendContextImpl.printContext() {
        runReadAction {
            if (this.frameProxy == null) {
                return@runReadAction println("Context thread is null", ProcessOutputTypes.SYSTEM)
            }

            val sourcePosition = PositionUtil.getSourcePosition(this)
            println(renderSourcePosition(sourcePosition), ProcessOutputTypes.SYSTEM)
        }
    }

    protected fun renderSourcePosition(sourcePosition: SourcePosition?): String {
        if (sourcePosition == null) {
            return "null"
        }

        val virtualFile = sourcePosition.file.originalFile.virtualFile ?: sourcePosition.file.viewProvider.virtualFile ?:
                          return "VirtualFile for position is null"

        val libraryEntry = LibraryUtil.findLibraryEntry(virtualFile, project)
        if (libraryEntry != null && (libraryEntry is JdkOrderEntry || libraryEntry.presentableName == KOTLIN_LIBRARY_NAME)) {
            return FileUtil.getNameWithoutExtension(virtualFile.name) + ".!EXT!"
        }

        return virtualFile.name + ":" + (sourcePosition.line + 1)
    }

    protected fun finish() {
        doOnBreakpoint {
            resume(this)
        }
    }

    override fun createBreakpoints(file: PsiFile?) {
        if (file == null) return

        val document = runReadAction { PsiDocumentManager.getInstance(myProject).getDocument(file) } ?: return
        val breakpointManager = XDebuggerManager.getInstance(myProject).breakpointManager
        val kotlinFieldBreakpointType = findBreakpointType(KotlinFieldBreakpointType::class.java)
        val virtualFile = file.virtualFile

        val runnable = {
            var offset = -1
            while (true) {
                val fileText = document.text
                offset = fileText.indexOf("point!", offset + 1)
                if (offset == -1) break

                val commentLine = document.getLineNumber(offset)

                val comment = fileText.substring(document.getLineStartOffset(commentLine), document.getLineEndOffset(commentLine)).trim()

                val lineIndex = commentLine + 1

                if (comment.startsWith("//FieldWatchpoint!")) {
                    val javaBreakpoint = createBreakpointOfType(
                            breakpointManager,
                            kotlinFieldBreakpointType as XLineBreakpointType<XBreakpointProperties<*>>,
                            lineIndex,
                            virtualFile)
                    if (javaBreakpoint is KotlinFieldBreakpoint) {
                        val fieldName = comment.substringAfter("//FieldWatchpoint! (").substringBefore(")")
                        javaBreakpoint.setFieldName(fieldName)
                        javaBreakpoint.setWatchAccess(fileText.getValueForSetting("WATCH_FIELD_ACCESS", true))
                        javaBreakpoint.setWatchModification(fileText.getValueForSetting("WATCH_FIELD_MODIFICATION", true))
                        javaBreakpoint.setWatchInitialization(fileText.getValueForSetting("WATCH_FIELD_INITIALISATION", false))
                        BreakpointManager.addBreakpoint(javaBreakpoint)
                        println("KotlinFieldBreakpoint created at ${file.virtualFile.name}:${lineIndex + 1}", ProcessOutputTypes.SYSTEM)
                    }
                }
                else if (comment.startsWith("//Breakpoint!")) {
                    val ordinal = getPropertyFromComment(comment, "lambdaOrdinal")?.toInt()
                    val condition = getPropertyFromComment(comment, "condition")
                    createLineBreakpoint(breakpointManager, file, lineIndex, ordinal, condition)
                }
                else {
                    throw AssertionError("Cannot create breakpoint at line ${lineIndex + 1}")
                }
            }
        }

        if (!SwingUtilities.isEventDispatchThread()) {
            DebuggerInvocationUtil.invokeAndWait(myProject, runnable, ModalityState.defaultModalityState())
        }
        else {
            runnable.invoke()
        }
    }

    private fun getPropertyFromComment(comment: String, propertyName: String): String? {
        if (comment.contains("$propertyName = ")) {
            val result = comment.substringAfter("$propertyName = ")
            if (result.contains(", ")) {
                return result.substringBefore(", ")
            }
            if (result.contains(")")) {
                return result.substringBefore(")")
            }
        }
        return null
    }

    private fun createLineBreakpoint(
            breakpointManager: XBreakpointManager,
            file: PsiFile,
            lineIndex: Int,
            lambdaOrdinal: Int?,
            condition: String?
    ) {
        val kotlinLineBreakpointType = findBreakpointType(KotlinLineBreakpointType::class.java)
        val javaBreakpoint = createBreakpointOfType(
                breakpointManager,
                kotlinLineBreakpointType  as XLineBreakpointType<XBreakpointProperties<*>>,
                lineIndex,
                file.virtualFile)
        if (javaBreakpoint is LineBreakpoint<*>) {
            val properties = javaBreakpoint.xBreakpoint.properties as? JavaLineBreakpointProperties ?: return
            var suffix = ""
            if (lambdaOrdinal != null) {
                if (lambdaOrdinal != -1) {
                    properties.lambdaOrdinal = lambdaOrdinal - 1
                }
                else {
                    properties.lambdaOrdinal = lambdaOrdinal
                }
                suffix += " lambdaOrdinal = $lambdaOrdinal"
            }
            if (condition != null) {
                javaBreakpoint.setCondition(TextWithImportsImpl(CodeFragmentKind.EXPRESSION, condition))
                suffix += " condition = $condition"
            }

            BreakpointManager.addBreakpoint(javaBreakpoint)
            println("LineBreakpoint created at ${file.virtualFile.name}:${lineIndex + 1}$suffix", ProcessOutputTypes.SYSTEM)
        }
    }

    private fun createBreakpointOfType(
            breakpointManager: XBreakpointManager,
            breakpointType: XLineBreakpointType<XBreakpointProperties<*>>,
            lineIndex: Int,
            virtualFile: VirtualFile
    ): Breakpoint<out JavaBreakpointProperties<*>>? {
        if (!breakpointType.canPutAt(virtualFile, lineIndex, myProject)) return null
        val xBreakpoint = runWriteAction {
            breakpointManager.addLineBreakpoint(
                    breakpointType,
                    virtualFile.url,
                    lineIndex,
                    breakpointType.createBreakpointProperties(virtualFile, lineIndex)
            )
        }
        return BreakpointManager.getJavaBreakpoint(xBreakpoint)
    }

    private inline fun <reified T> findBreakpointType(javaClass: Class<T>): T {
        val kotlinFieldBreakpointTypeClass = javaClass as Class<out XBreakpointType<XBreakpoint<XBreakpointProperties<*>>, XBreakpointProperties<*>>>
        return XDebuggerUtil.getInstance().findBreakpointType(kotlinFieldBreakpointTypeClass) as T
    }

    protected fun createAdditionalBreakpoints(fileText: String) {
        val breakpoints = InTextDirectivesUtils.findLinesWithPrefixesRemoved(fileText, "// ADDITIONAL_BREAKPOINT: ")
        for (breakpoint in breakpoints) {
            val position = breakpoint.split(".kt:")
            assert(position.size == 2) { "Couldn't parse position from test directive: directive = $breakpoint" }
            var lineMarker = position[1]
            var ordinal: Int? = null
            if (lineMarker.contains(":(") && lineMarker.endsWith(")")) {
                val lineMarkerAndOrdinal = lineMarker.split(":(")
                lineMarker = lineMarkerAndOrdinal[0]
                ordinal = lineMarkerAndOrdinal[1].substringBefore(")").toInt()
            }
            createBreakpoint(position[0], lineMarker, ordinal)
        }
    }

    private fun createBreakpoint(fileName: String, lineMarker: String, ordinal: Int?) {
        val project = project!!
        val sourceFiles = runReadAction {
            FilenameIndex.getAllFilesByExt(project, "kt").filter {
                it.name.contains(fileName) &&
                it.contentsToByteArray().toString(Charsets.UTF_8).contains(lineMarker)
            }
        }

        assert(sourceFiles.size == 1) { "One source file should be found: name = $fileName, sourceFiles = $sourceFiles" }

        val runnable = Runnable() {
            val psiSourceFile = PsiManager.getInstance(project).findFile(sourceFiles.first())!!

            val breakpointManager = XDebuggerManager.getInstance(myProject).breakpointManager
            val document = PsiDocumentManager.getInstance(project).getDocument(psiSourceFile)!!

            val index = psiSourceFile.text!!.indexOf(lineMarker)
            val lineNumber = document.getLineNumber(index) + 1 // lineMarker is for previous line

            createLineBreakpoint(breakpointManager, psiSourceFile, lineNumber, ordinal, null)
        }

        DebuggerInvocationUtil.invokeAndWait(project, runnable, ModalityState.defaultModalityState())
    }
}
