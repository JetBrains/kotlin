/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

package org.jetbrains.jet.asJava

import org.jetbrains.jet.lang.resolve.BindingContext
import org.jetbrains.jet.lang.psi.JetClassOrObject
import com.intellij.openapi.project.Project
import org.jetbrains.jet.lang.psi.JetPsiUtil
import org.jetbrains.jet.plugin.caches.resolve.KotlinCacheManager
import org.jetbrains.jet.plugin.project.AnalyzerFacadeWithCache

class KotlinLightClassContextCache(val project: Project) {
    public fun getLightClassBindingContext(classOrObject: JetClassOrObject): BindingContext {
        if (JetPsiUtil.isLocal(classOrObject)) {
            return AnalyzerFacadeWithCache.getContextForElement(classOrObject)
        }

        return KotlinCacheManager.getInstance(project).getPossiblyIncompleteDeclarationsForLightClassGeneration().getBindingContext()
    }

}
