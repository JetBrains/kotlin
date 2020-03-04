/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.statistics

object NewProjectWizardsFUSCollector {
    data class WizardDescription(val name: String, val group: String, val isKotlinDsl: Boolean?)

    private val classToNameMap = mapOf(
        "KotlinModuleBuilder__JVM_(JVM_1_6)" to "JVM | IDEA",
        "KotlinModuleBuilder__JS" to "JS | IDEA",
        "KotlinGradleNativeMultiplatformModuleBuilder" to "Native | Gradle",
        "KotlinGradleSharedMultiplatformModuleBuilder" to "Multiplatform Library | Gradle",
        "KotlinGradleWebMultiplatformModuleBuilder" to "JS Client and JVM Server | Gradle",
        "KotlinGradleMobileMultiplatformModuleBuilder" to "Mobile Android/iOS | Gradle",
        "KotlinGradleMobileSharedMultiplatformModuleBuilder" to "Mobile Shared Library | Gradle",
        "KotlinJavaFrameworkSupportProvider" to "Kotlin/JVM",
        "GradleKotlinJavaFrameworkSupportProvider" to "Kotlin/JVM",
        "GradleKotlinJSFrameworkSupportProvider" to "Kotlin/JS",
        "GradleKotlinJSBrowserFrameworkSupportProvider" to "Kotlin/JS for browser",
        "GradleKotlinJSNodeFrameworkSupportProvider" to "Kotlin/JS for Node.js",
        "GradleKotlinMPPFrameworkSupportProvider" to "Kotlin/Multiplatform_aggregated",
        "GradleKotlinMPPSourceSetsFrameworkSupportProvider" to "Kotlin/Multiplatform",
        "KotlinDslGradleKotlinJavaFrameworkSupportProvider" to "Kotlin/JVM",
        "KotlinDslGradleKotlinJSBrowserFrameworkSupportProvider" to "Kotlin/JS for browser",
        "KotlinDslGradleKotlinJSNodeFrameworkSupportProvider" to "Kotlin/JS for Node.js",
        "KotlinDslGradleKotlinMPPFrameworkSupportProvider" to "Kotlin/Multiplatform"
    )

    private fun parseWizardClass(className: String): WizardDescription {
        val wizardName = classToNameMap[className] ?: "unknown"

        val wizardGroup = if (wizardName == "unknown") "unknown"
        else if (className.startsWith("KotlinJava")) "Java"
        else if (className.startsWith("Gradle") || className.startsWith("KotlinDslGradle")) "Gradle"
        else "Kotlin"

        val isBasedOnKotlinDSL = if (wizardName != "unknown") className.startsWith("KotlinDsl")
        else null

        return WizardDescription(wizardName, wizardGroup, isBasedOnKotlinDSL)
    }

    fun log(wizardClassName: String) {
        val wizardDescription = parseWizardClass(wizardClassName)

        val contextData = mapOf(
            "name" to wizardDescription.name,
            "group" to wizardDescription.group,
            "isKotlinDsl" to wizardDescription.isKotlinDsl.toString()
        )

        KotlinFUSLogger.log(FUSEventGroups.NPWizards, "Finished", contextData)
    }


}