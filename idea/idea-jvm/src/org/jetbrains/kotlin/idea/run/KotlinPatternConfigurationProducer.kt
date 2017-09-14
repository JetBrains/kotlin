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

package org.jetbrains.kotlin.idea.run

import com.intellij.execution.actions.ConfigurationContext
import com.intellij.execution.actions.ConfigurationFromContext
import com.intellij.execution.junit.JUnitConfiguration
import com.intellij.execution.junit.PatternConfigurationProducer
import com.intellij.execution.junit.TestClassConfigurationProducer
import com.intellij.openapi.util.Ref
import com.intellij.psi.PsiElement
import com.intellij.psi.search.PsiElementProcessor
import org.jetbrains.kotlin.asJava.toLightClass
import org.jetbrains.kotlin.psi.KtClassOrObject

class KotlinPatternConfigurationProducer : PatternConfigurationProducer() {
    override fun setupConfigurationFromContext(
            configuration: JUnitConfiguration,
            context: ConfigurationContext,
            sourceElement: Ref<PsiElement>
    ): Boolean {
        return super.setupConfigurationFromContext(configuration, context, sourceElement)
    }

    override fun collectTestMembers(
            psiElements: Array<out PsiElement>,
            checkAbstract: Boolean,
            checkIsTest: Boolean,
            collectingProcessor: PsiElementProcessor.CollectElements<PsiElement>
    ) {
        val adjustedElements = psiElements.mapNotNull { if (it is KtClassOrObject) it.toLightClass() else it }.toTypedArray()
        super.collectTestMembers(adjustedElements, checkAbstract, checkIsTest, collectingProcessor)
    }

    override fun shouldReplace(self: ConfigurationFromContext, other: ConfigurationFromContext): Boolean {
        return other.isProducedBy(PatternConfigurationProducer::class.java)
               || other.isProducedBy(TestClassConfigurationProducer::class.java)
               || other.isProducedBy(KotlinJUnitRunConfigurationProducer::class.java)
    }
}
