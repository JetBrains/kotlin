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

package org.jetbrains.kotlin.asJava

import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.asJava.builder.LightClassBuilderResult
import org.jetbrains.kotlin.asJava.builder.LightClassConstructionContext
import org.jetbrains.kotlin.asJava.builder.LightClassDataHolder
import org.jetbrains.kotlin.asJava.classes.KtUltraLightClass
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.BindingContext

typealias LightClassBuilder = (LightClassConstructionContext) -> LightClassBuilderResult

abstract class LightClassGenerationSupport {
    abstract fun createDataHolderForClass(classOrObject: KtClassOrObject, builder: LightClassBuilder): LightClassDataHolder.ForClass

    abstract fun createDataHolderForFacade(files: Collection<KtFile>, builder: LightClassBuilder): LightClassDataHolder.ForFacade

    abstract fun createDataHolderForScript(script: KtScript, builder: LightClassBuilder): LightClassDataHolder.ForScript

    abstract fun resolveToDescriptor(declaration: KtDeclaration): DeclarationDescriptor?

    abstract fun analyze(element: KtElement): BindingContext

    abstract fun analyzeWithContent(element: KtClassOrObject): BindingContext

    abstract fun createUltraLightClass(element: KtClassOrObject): KtUltraLightClass?

    companion object {
        @JvmStatic
        fun getInstance(project: Project): LightClassGenerationSupport {
            return ServiceManager.getService(project, LightClassGenerationSupport::class.java)
        }
    }
}
