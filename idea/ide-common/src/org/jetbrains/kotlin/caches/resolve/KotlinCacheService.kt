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

package org.jetbrains.kotlin.caches.resolve

import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import org.jetbrains.kotlin.analyzer.ModuleInfo
import org.jetbrains.kotlin.idea.resolve.ResolutionFacade
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.resolve.diagnostics.KotlinSuppressCache

interface KotlinCacheService {
    companion object {
        fun getInstance(project: Project): KotlinCacheService = ServiceManager.getService(project, KotlinCacheService::class.java)!!
    }

    fun getResolutionFacade(elements: List<KtElement>): ResolutionFacade
    fun getResolutionFacadeByFile(file: PsiFile, platform: TargetPlatform): ResolutionFacade?

    fun getSuppressionCache(): KotlinSuppressCache
    fun getResolutionFacadeByModuleInfo(moduleInfo: ModuleInfo, platform: TargetPlatform): ResolutionFacade?
}