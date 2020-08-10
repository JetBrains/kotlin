package com.jetbrains.mobile.execution.testing

import com.intellij.execution.actions.ConfigurationContext
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.ConfigurationTypeUtil
import com.intellij.openapi.util.Ref
import com.intellij.psi.PsiElement
import com.jetbrains.cidr.execution.testing.CidrTestWithScopeElementsRunConfigurationProducer
import com.jetbrains.cidr.execution.testing.xctest.OCUnitTestObject
import com.jetbrains.mobile.execution.MobileBuildTargetRunConfigurationBinder
import com.jetbrains.mobile.execution.MobileBuildConfiguration
import com.jetbrains.mobile.execution.MobileBuildTarget
import com.jetbrains.mobile.isApple
import com.jetbrains.mobile.isMobileAppTest
import org.jetbrains.kotlin.idea.util.projectStructure.allModules

class AppleXCTestRunConfigurationProducer : CidrTestWithScopeElementsRunConfigurationProducer<
        MobileBuildConfiguration,
        MobileBuildTarget,
        MobileTestRunConfiguration,
        OCUnitTestObject,
        AppleXCTestFramework>(MobileBuildTargetRunConfigurationBinder, AppleXCTestFramework::class.java) {

    override fun getConfigurationFactory(): ConfigurationFactory =
        ConfigurationTypeUtil.findConfigurationType(MobileTestRunConfigurationType::class.java).configurationFactories[0]

    override fun setupConfigurationFromContext(
        configuration: MobileTestRunConfiguration,
        context: ConfigurationContext,
        sourceElement: Ref<PsiElement>
    ): Boolean {
        configuration.module = configuration.project.allModules().find { it.isApple && it.isMobileAppTest }
        return super.setupConfigurationFromContext(configuration, context, sourceElement)
    }
}
