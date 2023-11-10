/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.test.configurators

import com.intellij.mock.MockProject
import org.jetbrains.kotlin.analysis.test.framework.test.configurators.AnalysisApiTestServiceRegistrar
import org.jetbrains.kotlin.scripting.compiler.plugin.definitions.CliScriptDefinitionProvider
import org.jetbrains.kotlin.scripting.definitions.ScriptDefinitionProvider
import org.jetbrains.kotlin.test.services.TestServices

object AnalysisApiScriptTestServiceRegistrar : AnalysisApiTestServiceRegistrar() {
    override fun registerProjectModelServices(project: MockProject, testServices: TestServices) {
        project.registerService(ScriptDefinitionProvider::class.java, CliScriptDefinitionProvider())
    }
}
