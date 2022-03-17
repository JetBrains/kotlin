/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.tests.analysis.api.dsl

import org.jetbrains.annotations.NotNull
import org.jetbrains.kotlin.analysis.test.framework.AnalysisApiTestConfiguratorService
import org.jetbrains.kotlin.generators.MethodGenerator
import org.jetbrains.kotlin.generators.model.MethodModel
import org.jetbrains.kotlin.utils.Printer
import kotlin.reflect.KClass

object FrontendConfiguratorTestGenerator : MethodGenerator<FrontendConfiguratorTestModel>() {
    override val kind: MethodModel.Kind get() = FrontendConfiguratorTestModelKind

    override fun generateSignature(method: FrontendConfiguratorTestModel, p: Printer): Unit = with(p) {
        println("@NotNull")
        println("@Override")
        print("public FrontendApiTestConfiguratorService getConfigurator()")
    }

    override fun generateBody(method: FrontendConfiguratorTestModel, p: Printer): Unit = with(p) {
        print("return ")
        printWithNoIndent(method.frontendConfiguratorClass.simpleName)
        printWithNoIndent(".INSTANCE")
        printlnWithNoIndent(";")
    }
}

object FrontendConfiguratorTestModelKind : MethodModel.Kind()

class FrontendConfiguratorTestModel(val frontendConfiguratorClass: KClass<out AnalysisApiTestConfiguratorService>) : MethodModel {
    override val kind: MethodModel.Kind get() = FrontendConfiguratorTestModelKind
    override val name: String get() = "getConfigurator"
    override val dataString: String? get() = null
    override val tags: List<String> get() = emptyList()

    override fun isTestMethod(): Boolean = false
    override fun shouldBeGeneratedForInnerTestClass(): Boolean = false

    override fun imports(): Collection<Class<*>> {
        return buildList {
            add(NotNull::class.java)
            add(AnalysisApiTestConfiguratorService::class.java)
            add(frontendConfiguratorClass.java)
        }
    }
}