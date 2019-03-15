/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.statistics

// Note: along with adding a trigger to this enum you sould also add its GROUP_ID to plugin.xml and get it whitelisted
// (see https://confluence.jetbrains.com/display/FUS/IntelliJ+Reporting+API).
enum class KotlinEventTrigger(groupIdSufix: String) {
    KotlinGradleTargetTrigger("gradle.target"),
    KotlinMavenTargetTrigger("maven.target"),
    KotlinJPSTargetTrigger("jps.target"),
    KotlinProjectLibraryUsageTrigger("gradle.library"),
    KotlinIdeRefactoringTrigger("ide.action.refactoring"),
    KotlinIdeNewFileTemplateTrigger("ide.newFileTempl");

    val GROUP_ID: String = "kotlin.$groupIdSufix"
}