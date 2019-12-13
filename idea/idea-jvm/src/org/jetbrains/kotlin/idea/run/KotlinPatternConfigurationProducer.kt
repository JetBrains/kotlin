/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.run

import com.intellij.execution.actions.ConfigurationContext
import com.intellij.execution.actions.ConfigurationFromContext
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.junit.JUnitConfiguration
import com.intellij.execution.junit.PatternConfigurationProducer
import com.intellij.execution.junit.TestClassConfigurationProducer
import com.intellij.openapi.util.Ref
import com.intellij.psi.PsiElement
import com.intellij.psi.search.PsiElementProcessor
import org.jetbrains.kotlin.asJava.toLightClass
import org.jetbrains.kotlin.psi.KtClassOrObject

class KotlinPatternConfigurationProducer : PatternConfigurationProducer() {
    override fun getConfigurationFactory(): ConfigurationFactory {
        return KotlinJUnitConfigurationType.instance.factory
    }

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
