/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

@file:JvmName("UltimateBuildSrc")
@file:JvmMultifileClass

package org.jetbrains.kotlin.ultimate

import org.gradle.api.Project
import org.gradle.kotlin.dsl.extra
import java.io.FileReader
import java.util.*

val Project.kotlinVersion get() = p( /* Kotlin version enforced by Big Kotlin */ "kotlinVersion") {
    // otherwise: take Kotlin version from properties
    kotlinVersionFull.substringBefore('-')
}

val Project.kotlinVersionFull get() = p("versions.kotlin4cidr")
val Project.kotlinPluginBuildNumber get() = kotlinVersionFull.split("-release", limit = 2).takeIf { it.size == 2 }?.let { "${it[0]}-release" } ?: kotlinVersionFull
val Project.kotlinPluginVersion get() = p("versions.kotlin4cidr.plugin")
val Project.kotlinVersionRepo get() = p("versions.kotlin4cidr.repo")

val Project.appcodeVersion get() = p("versions.appcode")
val Project.appcodeVersionStrict get() = p("versions.appcode.strict").toBoolean()
val Project.appcodeVersionRepo get() = p("versions.appcode.repo")

val Project.clionVersion get() = p("versions.clion")
val Project.clionVersionStrict get() = p("versions.clion.strict").toBoolean()
val Project.clionVersionRepo get() = p("versions.clion.repo")

// Note: "appcodePluginVersion" and "clionPluginVersion" have different format and semantics from
// "pluginVersion" used in IJ and AS plugins.
val Project.appcodePluginVersion get() = findProperty("appcodePluginVersion") ?: "beta-1"
val Project.appcodePluginVersionFull get() = "$kotlinVersion-AppCode-$appcodePluginVersion-$appcodeVersion"

val Project.clionPluginVersion get() = findProperty("clionPluginVersion") ?: "beta-1"
val Project.clionPluginVersionFull get() = "$kotlinVersion-CLion-$clionPluginVersion-$clionVersion"

val Project.intellijSdkVersion get() = p("versions.intellijSdk")
val Project.markdownVersion get() = p("versions.markdown")

internal fun Project.p(name: String, defaultValue: (Project.() -> String)? = null) =
    if (!rootProject.extra.has(name) && defaultValue != null)
        defaultValue()
    else
        rootProject.extra[name].toString()

fun Project.setupVersionProperties() {
    FileReader(ultimateProject(":").file("versions.properties")).use {
        val properties = Properties()
        properties.load(it)
        properties.forEach { (k, v) ->
            rootProject.extra[k.toString()] = v
        }
    }
}
