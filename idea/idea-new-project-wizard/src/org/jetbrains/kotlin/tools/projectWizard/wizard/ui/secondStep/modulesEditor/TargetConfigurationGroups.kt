/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.tools.projectWizard.wizard.ui.secondStep.modulesEditor

import org.jetbrains.kotlin.idea.KotlinIcons
import org.jetbrains.kotlin.tools.projectWizard.KotlinNewProjectWizardBundle
import org.jetbrains.kotlin.tools.projectWizard.moduleConfigurators.*
import org.jetbrains.kotlin.tools.projectWizard.plugins.kotlin.ModuleSubType
import org.jetbrains.kotlin.tools.projectWizard.plugins.kotlin.ModuleType

object TargetConfigurationGroups {
    val JS = FinalTargetConfiguratorGroup(
        ModuleType.js.projectTypeName,
        ModuleType.js,
        KotlinIcons.Wizard.JS,
        listOf(
            JsBrowserTargetConfigurator,
            JsNodeTargetConfigurator
        )
    )

    object NATIVE {
        val LINUX = FinalTargetConfiguratorGroup(
            KotlinNewProjectWizardBundle.message("module.configuration.group.linux"),
            ModuleType.native,
            KotlinIcons.Wizard.LINUX,
            listOf(
                RealNativeTargetConfigurator.configuratorsByModuleType.getValue(ModuleSubType.linuxX64),
                RealNativeTargetConfigurator.configuratorsByModuleType.getValue(ModuleSubType.linuxArm32Hfp),
                RealNativeTargetConfigurator.configuratorsByModuleType.getValue(ModuleSubType.linuxMips32),
                RealNativeTargetConfigurator.configuratorsByModuleType.getValue(ModuleSubType.linuxMipsel32)
            )
        )

        val WINDOWS = FinalTargetConfiguratorGroup(
            KotlinNewProjectWizardBundle.message("module.configuration.group.windows.mingw"),
            ModuleType.native,
            KotlinIcons.Wizard.WINDOWS,
            listOf(
                RealNativeTargetConfigurator.configuratorsByModuleType.getValue(ModuleSubType.mingwX64),
                RealNativeTargetConfigurator.configuratorsByModuleType.getValue(ModuleSubType.mingwX86)
            )
        )

        val MAC = FinalTargetConfiguratorGroup(
            KotlinNewProjectWizardBundle.message("module.configuration.group.macos"),
            ModuleType.native,
            KotlinIcons.Wizard.MAC_OS,
            listOf(
                RealNativeTargetConfigurator.configuratorsByModuleType.getValue(ModuleSubType.macosX64)
            )
        )

        val IOS = FinalTargetConfiguratorGroup(
            KotlinNewProjectWizardBundle.message("module.configuration.group.ios"),
            ModuleType.native,
            KotlinIcons.Wizard.IOS,
            listOf(
                RealNativeTargetConfigurator.configuratorsByModuleType.getValue(ModuleSubType.iosArm32),
                RealNativeTargetConfigurator.configuratorsByModuleType.getValue(ModuleSubType.iosArm64),
                RealNativeTargetConfigurator.configuratorsByModuleType.getValue(ModuleSubType.iosX64)
            )
        )

        val ANDROID_NATIVE = FinalTargetConfiguratorGroup(
            KotlinNewProjectWizardBundle.message("module.configuration.group.android.native"),
            ModuleType.native,
            KotlinIcons.Wizard.ANDROID,
            listOf(
                RealNativeTargetConfigurator.configuratorsByModuleType.getValue(ModuleSubType.androidNativeArm64),
                RealNativeTargetConfigurator.configuratorsByModuleType.getValue(ModuleSubType.androidNativeArm32)
            )
        )

        val ALL = StepTargetConfiguratorGroup(
            ModuleType.native.projectTypeName,
            ModuleType.native,
            listOf(
                NativeForCurrentSystemTarget,
                LINUX,
                WINDOWS,
                MAC,
                IOS,
                ANDROID_NATIVE
            )
        )
    }

    val FIRST = FirstStepTargetConfiguratorGroup(
        listOf(
            CommonTargetConfigurator,
            JvmTargetConfigurator,
            NATIVE.ALL,
            JS,
            AndroidTargetConfigurator
        )
    )
}