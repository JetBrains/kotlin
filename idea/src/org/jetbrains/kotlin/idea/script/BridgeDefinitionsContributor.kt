/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.script

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.extensions.Extensions
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageLocation
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.idea.core.script.ScriptDefinitionSourceAsContributor
import org.jetbrains.kotlin.idea.core.script.loadDefinitionsFromTemplates
import org.jetbrains.kotlin.scripting.definitions.ScriptDefinition
import org.jetbrains.kotlin.scripting.definitions.ScriptDefinitionsFromClasspathDiscoverySource
import org.jetbrains.kotlin.scripting.definitions.reporter
import kotlin.script.experimental.intellij.ScriptDefinitionsProvider
import kotlin.script.experimental.jvm.defaultJvmScriptingHostConfiguration

class BridgeScriptDefinitionsContributor(private val project: Project) : ScriptDefinitionSourceAsContributor {
    override val id: String = "BridgeScriptDefinitionsContributor"

    override val definitions: Sequence<ScriptDefinition>
        get() {
            val extensions = Extensions.getArea(project).getExtensionPoint(ScriptDefinitionsProvider.EP_NAME).extensions
            val messageCollector = LoggingMessageCollector()
            return extensions.asSequence().flatMap { provider ->
                val explicitClasses = provider.getDefinitionClasses().toList()
                val classPath = provider.getDefinitionsClassPath().toList()
                val hostConfiguration = defaultJvmScriptingHostConfiguration
                val explicitDefinitions =
                    if (explicitClasses.isEmpty()) emptySequence()
                    else loadDefinitionsFromTemplates(explicitClasses, classPath, hostConfiguration).asSequence()
                val discoveredDefinitions =
                    if (provider.useDiscovery()) emptySequence()
                    else ScriptDefinitionsFromClasspathDiscoverySource(
                        classPath,
                        hostConfiguration,
                        messageCollector.reporter
                    ).definitions
                explicitDefinitions + discoveredDefinitions
            }
        }
}

private class LoggingMessageCollector : MessageCollector {

    private val log = Logger.getInstance("ScriptDefinitionsProviders")

    private var hasErrors = false

    override fun clear() {
        hasErrors = false
    }

    override fun report(severity: CompilerMessageSeverity, message: String, location: CompilerMessageLocation?) {
        when (severity) {
            CompilerMessageSeverity.ERROR, CompilerMessageSeverity.EXCEPTION -> {
                log.error(message)
                hasErrors = true
            }
            CompilerMessageSeverity.WARNING, CompilerMessageSeverity.STRONG_WARNING -> log.warn(message)
            else -> log.info(message)
        }
    }

    override fun hasErrors(): Boolean = hasErrors
}
