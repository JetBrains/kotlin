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

package org.jetbrains.kotlin.idea.facet

import com.intellij.codeHighlighting.TextEditorHighlightingPass
import com.intellij.codeHighlighting.TextEditorHighlightingPassFactory
import com.intellij.codeHighlighting.TextEditorHighlightingPassRegistrar
import com.intellij.framework.detection.DetectedFrameworkDescription
import com.intellij.framework.detection.DetectionExcludesConfiguration
import com.intellij.framework.detection.impl.*
import com.intellij.framework.detection.impl.exclude.DetectionExcludesConfigurationImpl
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.AbstractProjectComponent
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupManager
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.util.indexing.FileBasedIndex
import com.intellij.util.ui.update.MergingUpdateQueue
import com.intellij.util.ui.update.Update
import java.util.*

// Based on com.intellij.framework.detection.impl.FrameworkDetectionManager
class FacetConfigurator(
        project: Project,
        highlightingPassRegistrar: TextEditorHighlightingPassRegistrar
) : AbstractProjectComponent(project), FrameworkDetectionIndexListener, TextEditorHighlightingPassFactory {
    private val myDetectionUpdate = object : Update("detection") {
        override fun run() {
            doRunDetection()
        }
    }
    private @Volatile var shouldDetect = false
    private var myDetectionQueue: MergingUpdateQueue? = null
    private var myDetectedFrameworksData: DetectedFrameworksData? = null

    init {
        highlightingPassRegistrar.registerTextEditorHighlightingPass(this, TextEditorHighlightingPassRegistrar.Anchor.LAST, -1, false, false)
    }

    override fun initComponent() {
        if (!myProject.isDefault && !ApplicationManager.getApplication().isUnitTestMode) {
            doInitialize()
        }
    }

    fun doInitialize() {
        myDetectionQueue = MergingUpdateQueue("FrameworkDetectionQueue", 500, true, null, myProject)
        if (ApplicationManager.getApplication().isUnitTestMode) {
            myDetectionQueue!!.isPassThrough = false
            myDetectionQueue!!.hideNotify()
        }
        myDetectedFrameworksData = DetectedFrameworksData(myProject)
        FrameworkDetectionIndex.getInstance().addListener(this, myProject)
        myProject.messageBus.connect().subscribe(
                DumbService.DUMB_MODE,
                object : DumbService.DumbModeListener {
                    override fun enteredDumbMode() = myDetectionQueue!!.suspend()
                    override fun exitDumbMode() = myDetectionQueue!!.resume()
                }
        )
    }

    override fun projectOpened() {
        StartupManager.getInstance(myProject).registerPostStartupActivity {
            shouldDetect = true
            queueDetection()
        }
    }

    override fun disposeComponent() {
        doDispose()
    }

    fun doDispose() {
        if (myDetectedFrameworksData != null) {
            myDetectedFrameworksData!!.saveDetected()
            myDetectedFrameworksData = null
        }
    }

    override fun fileUpdated(file: VirtualFile, detectorId: Int) {
        if (detectorId != KotlinFrameworkDetector.detectorIndex) return
        shouldDetect = true
        queueDetection()
    }

    private fun queueDetection() {
        if (myDetectionQueue != null) {
            myDetectionQueue!!.queue(myDetectionUpdate)
        }
    }

    override fun createHighlightingPass(file: PsiFile, editor: Editor): TextEditorHighlightingPass? {
        val detectors = FrameworkDetectorRegistry.getInstance().getDetectorIds(file.fileType)
        if (!detectors.isEmpty()) {
            return FrameworkDetectionHighlightingPass(editor, detectors)
        }
        return null
    }

    private fun doRunDetection() {
        if (!shouldDetect) return
        shouldDetect = false

        /*
        val index = FileBasedIndex.getInstance()
        val newDescriptions = ArrayList<DetectedFrameworkDescription>()
        val oldDescriptions = ArrayList<DetectedFrameworkDescription>()
        val excludesConfiguration = DetectionExcludesConfiguration.getInstance(myProject)

        val id = KotlinFrameworkDetector.detectorIndex
        val frameworks = runDetector(id, index, excludesConfiguration, true)
        oldDescriptions.addAll(frameworks)
        val updated = myDetectedFrameworksData!!.updateFrameworksList(id, frameworks)
        newDescriptions.addAll(updated)
        oldDescriptions.removeAll(updated)

        if (FrameworkDetectionUtil.removeDisabled(newDescriptions, oldDescriptions).isEmpty()) return

        val validFrameworks = getValidDetectedFrameworks().ifEmpty { return }
        for (validFramework in validFrameworks) {
            if (validFramework is FacetBasedDetectedFrameworkDescription<*, *>) {
                val module = ModuleManager.getInstance(myProject).findModuleByName(validFramework.get)
            }

        }
        FrameworkDetectionUtil.setupFrameworks(frameworks, PlatformModifiableModelsProvider(), DefaultModulesProvider(myProject))
        for (framework in validFrameworks) {
            myDetectedFrameworksData!!.putExistentFrameworkFiles(id, framework.relatedFiles)
        }
        */
    }

    private fun runDetector(detectorId: Int?,
                            index: FileBasedIndex,
                            excludesConfiguration: DetectionExcludesConfiguration,
                            processNewFilesOnly: Boolean): List<DetectedFrameworkDescription> {
        val acceptedFiles = index.getContainingFiles(FrameworkDetectionIndex.NAME, detectorId!!, GlobalSearchScope.projectScope(myProject))
        val filesToProcess: Collection<VirtualFile>
        if (processNewFilesOnly) {
            filesToProcess = myDetectedFrameworksData!!.retainNewFiles(detectorId, acceptedFiles)
        }
        else {
            filesToProcess = ArrayList(acceptedFiles)
        }
        val detector = FrameworkDetectorRegistry.getInstance().getDetectorById(detectorId) as? KotlinFrameworkDetector ?: return emptyList()

        (excludesConfiguration as DetectionExcludesConfigurationImpl).removeExcluded(filesToProcess, detector.frameworkType)
        val frameworks: List<DetectedFrameworkDescription>
        if (!filesToProcess.isEmpty()) {
            frameworks = detector.internalDetect(filesToProcess, FrameworkDetectionContextImpl(myProject))
        }
        else {
            frameworks = arrayListOf<DetectedFrameworkDescription>()
        }
        return frameworks
    }

    private fun getValidDetectedFrameworks(): List<DetectedFrameworkDescription> {
        val id = myDetectedFrameworksData!!.detectorsForDetectedFrameworks.singleOrNull() ?: return emptyList()
        val index = FileBasedIndex.getInstance()
        val excludesConfiguration = DetectionExcludesConfiguration.getInstance(myProject)
        val frameworks = runDetector(id, index, excludesConfiguration, false)
        return FrameworkDetectionUtil.removeDisabled(frameworks)
    }

    private fun ensureIndexIsUpToDate(detectors: Collection<Int>) {
        for (detectorId in detectors) {
            FileBasedIndex.getInstance().getValues(FrameworkDetectionIndex.NAME, detectorId, GlobalSearchScope.projectScope(myProject))
        }
    }

    private inner class FrameworkDetectionHighlightingPass(
            editor: Editor,
            private val myDetectors: Collection<Int>
    ) : TextEditorHighlightingPass(this@FacetConfigurator.myProject, editor.document, false) {

        override fun doCollectInformation(progress: ProgressIndicator) {
            ensureIndexIsUpToDate(myDetectors)
        }

        override fun doApplyInformationToEditor() {
        }
    }

    companion object {
        fun getInstance(project: Project) = project.getComponent(FacetConfigurator::class.java)!!
    }
}