/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.tests.analysis.api.dsl

import org.jetbrains.annotations.NotNull
import org.jetbrains.kotlin.analysis.test.framework.test.configurators.AnalysisApiTestConfiguratorFactory
import org.jetbrains.kotlin.analysis.test.framework.test.configurators.AnalysisApiTestConfiguratorFactoryData
import org.jetbrains.kotlin.analysis.test.framework.test.configurators.AnalysisApiTestConfigurator
import org.jetbrains.kotlin.generators.MethodGenerator
import org.jetbrains.kotlin.generators.model.MethodModel
import org.jetbrains.kotlin.utils.Printer
import kotlin.reflect.KClass

object FrontendConfiguratorTestGenerator : MethodGenerator<FrontendConfiguratorTestModel>() {
    override val kind: MethodModel.Kind get() = FrontendConfiguratorTestModelKind

    override fun generateSignature(method: FrontendConfiguratorTestModel, p: Printer): Unit = with(p) {
        println("@NotNull")
        println("@Override")
        print("public ${AnalysisApiTestConfigurator::class.simpleName} getConfigurator()")
    }

    override fun generateBody(method: FrontendConfiguratorTestModel, p: Printer): Unit = with(p) {
        print("return ")
        printWithNoIndent(method.frontendConfiguratorFactoryClass.simpleName)
        printlnWithNoIndent(".INSTANCE.createConfigurator(")
        pushIndent()
        println("new ", AnalysisApiTestConfiguratorFactoryData::class.simpleName, "(")
        pushIndent()
        println(method.data.frontend.asJavaCode(), ",")
        println(method.data.moduleKind.asJavaCode(), ",")
        println(method.data.analysisSessionMode.asJavaCode(), ",")
        println(method.data.analysisApiMode.asJavaCode())
        popIndent()
        println(")")
        popIndent()
        println(");")
    }

    private fun Enum<*>.asJavaCode(): String = "${this::class.simpleName}.${this.name}"
}

object FrontendConfiguratorTestModelKind : MethodModel.Kind()


class FrontendConfiguratorTestModel(
    val frontendConfiguratorFactoryClass: KClass<out AnalysisApiTestConfiguratorFactory>,
    val data: AnalysisApiTestConfiguratorFactoryData
) : MethodModel {
    override val kind: MethodModel.Kind get() = FrontendConfiguratorTestModelKind
    override val name: String get() = "getConfigurator"
    override val dataString: String? get() = null
    override val tags: List<String> get() = emptyList()

    override fun isTestMethod(): Boolean = false
    override fun shouldBeGeneratedForInnerTestClass(): Boolean = false

    override fun imports(): Collection<Class<*>> {
        return buildList {
            add(NotNull::class.java)
            add(frontendConfiguratorFactoryClass.java)
            add(AnalysisApiTestConfiguratorFactoryData::class.java)
            add(AnalysisApiTestConfigurator::class.java)
            add(data.moduleKind::class.java)

            add(data.frontend::class.java)
            add(data.analysisSessionMode::class.java)
            add(data.analysisApiMode::class.java)
        }
    }
}