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

import com.intellij.framework.detection.DetectedFrameworkDescription
import com.intellij.framework.detection.FacetBasedFrameworkDetector
import com.intellij.framework.detection.FileContentPattern
import com.intellij.framework.detection.FrameworkDetectionContext
import com.intellij.framework.detection.impl.FrameworkDetectorRegistry
import com.intellij.openapi.roots.ModifiableRootModel
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.idea.project.getAndCacheLanguageLevelByDependencies

class KotlinFrameworkDetector : FacetBasedFrameworkDetector<KotlinFacet, KotlinFacetConfiguration>(DETECTOR_ID) {
    companion object {
        val DETECTOR_ID = "kotlin"

        val detectorIndex by lazy { FrameworkDetectorRegistry.getInstance().getDetectorId(KotlinFrameworkDetector()) }
    }

    override fun getFacetType() = KotlinFacetType.INSTANCE

    override fun createSuitableFilePattern() = FileContentPattern.fileContent()

    override fun getFileType() = KotlinFileType.INSTANCE

    // Suppress detection to avoid notification via FrameworkDetectionManager (FacetConfigurator is used instead)
    override fun detect(
            newFiles: Collection<VirtualFile>,
            context: FrameworkDetectionContext
    ) = ArrayList<DetectedFrameworkDescription>()

    fun internalDetect(
            newFiles: Collection<VirtualFile>,
            context: FrameworkDetectionContext
    ) = super.detect(newFiles, context)

    override fun setupFacet(facet: KotlinFacet, model: ModifiableRootModel?) {
        model?.module?.let { module ->
            facet.configuration.settings.initializeIfNeeded(module, model)
            module.getAndCacheLanguageLevelByDependencies()
        }
    }
}
