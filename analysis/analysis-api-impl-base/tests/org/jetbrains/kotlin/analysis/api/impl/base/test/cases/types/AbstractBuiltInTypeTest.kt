/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.test.cases.types

import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.types.KtType
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.directives.model.SimpleDirectivesContainer
import org.jetbrains.kotlin.test.directives.model.singleValue
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices
import java.util.Locale

abstract class AbstractBuiltInTypeTest : AbstractTypeTest() {

    override fun configureTest(builder: TestConfigurationBuilder) {
        super.configureTest(builder)
        builder.apply {
            useDirectives(Directives)
        }
    }

    context(KtAnalysisSession)
    override fun getType(ktFile: KtFile, module: TestModule, testServices: TestServices): KtType {
        val builtInTypeName = module.directives.singleValue(Directives.BUILTIN_TYPE_NAME)
        val typeMethod = builtinTypes::class.java.methods.singleOrNull {
            it.name == "get${builtInTypeName.uppercase(Locale.US)}"
        }!!
        typeMethod.isAccessible = true
        return typeMethod.invoke(builtinTypes) as KtType
    }

    object Directives : SimpleDirectivesContainer() {
        val BUILTIN_TYPE_NAME by stringDirective("name of built in type")
    }
}