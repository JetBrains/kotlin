/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.scripting.gradle

import org.jetbrains.kotlin.scripting.definitions.ScriptCompilationConfigurationFromDefinition
import org.jetbrains.kotlin.scripting.definitions.ScriptDefinition
import org.jetbrains.kotlin.scripting.resolve.KotlinScriptDefinitionFromAnnotatedTemplate
import kotlin.script.experimental.api.*
import kotlin.script.experimental.host.ScriptingHostConfiguration
import kotlin.script.experimental.location.ScriptExpectedLocation

class GradleKotlinScriptDefinitionWrapper(
    hostConfiguration: ScriptingHostConfiguration,
    legacyDefinition: KotlinScriptDefinitionFromAnnotatedTemplate,
    gradleVersion: String,
) : ScriptDefinition.FromLegacy(hostConfiguration, legacyDefinition) {
    override val compilationConfiguration by lazy {
        ScriptCompilationConfigurationFromDefinition(
            hostConfiguration,
            legacyDefinition
        ).with {
            ScriptCompilationConfiguration.ide.acceptedLocations.put(listOf(ScriptAcceptedLocation.Project))
        }
    }

    override val canDefinitionBeSwitchedOff: Boolean = false
}