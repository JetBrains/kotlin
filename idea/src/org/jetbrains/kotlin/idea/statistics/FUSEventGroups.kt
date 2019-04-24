/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.statistics

// Note: along with adding a group to this enum you should also add its GROUP_ID to plugin.xml and get it whitelisted
// (see https://confluence.jetbrains.com/display/FUS/IntelliJ+Reporting+API).
enum class FUSEventGroups(groupIdSuffix: String) {
    GradleTarget("gradle.target"),
    MavenTarget("maven.target"),
    JPSTarget("jps.target"),
    Refactoring("ide.action.refactoring"),
    NewFileTemplate("ide.newFileTempl"),
    NPWizards("ide.npwizards"),
    DebugEval("ide.debugger.eval");

    val GROUP_ID: String = "kotlin.$groupIdSuffix"
}