/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.konan.util

import com.intellij.openapi.util.SystemInfo
import org.apache.velocity.VelocityContext
import org.apache.velocity.app.Velocity
import org.jetbrains.konan.util.CidrKotlinReleaseType.*
import java.io.StringWriter
import java.time.Year
import kotlin.math.max

val TEMPLATE_ARGUMENTS_CIDR_NEW_PROJECT = mapOf(
        "CIDR_MPP_PLATFORM" to mppPlatform,
        "CIDR_PLUGIN_VERSION" to pluginVersion,
        "CIDR_CUSTOM_PLUGIN_REPOS" to customPluginRepos,
        "CIDR_CURRENT_YEAR" to currentYear
)

fun mergeTemplate(template: String, templateArguments: Map<String, Any>): String = StringWriter().apply {
    Velocity.evaluate(VelocityContext(templateArguments), this, "", template)
}.toString()

private val mppPlatform get() = when {
    SystemInfo.isMac -> "macosX64"
    SystemInfo.isWindows -> "mingwX64"
    else -> "linuxX64"
}

private val pluginVersion get() = when (defaultCidrKotlinVersion.releaseType) {
    RELEASE -> defaultCidrKotlinVersion.kotlinVersion.toString()
    else -> defaultCidrKotlinVersion.toString()
}

private val customPluginRepos get() = when (defaultCidrKotlinVersion.releaseType) {
    RELEASE -> emptyList()
    EAP -> listOf(
            "https://dl.bintray.com/kotlin/kotlin-eap",
            "https://dl.bintray.com/kotlin/kotlin-dev"
    )
    DEV -> listOf(
            "https://dl.bintray.com/kotlin/kotlin-dev",
            "https://teamcity.jetbrains.com/guestAuth/app/rest/builds/buildType:(id:Kotlin_dev_Compiler),number:$defaultCidrKotlinVersion,branch:default:any/artifacts/content/maven/"
    )
}

private val currentYear get() = max(Year.now().value, 2019)
