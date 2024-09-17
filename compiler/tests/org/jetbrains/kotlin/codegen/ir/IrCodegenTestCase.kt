/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("JUnitTestCaseWithNoTests")

package org.jetbrains.kotlin.codegen.ir

import org.jetbrains.kotlin.codegen.*
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.JVMConfigurationKeys
import org.jetbrains.kotlin.config.JvmClosureGenerationScheme
import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.test.TargetBackend.JVM_IR

open class IrPackageGenTest : PackageGenTest() {
    override val backend: TargetBackend
        get() = JVM_IR
}

open class IrPrimitiveTypesTest : PrimitiveTypesTest() {
    override val backend: TargetBackend
        get() = JVM_IR
}

open class IrAnnotationGenTest : AnnotationGenTest() {
    override val backend: TargetBackend
        get() = JVM_IR
}

open class IrOuterClassGenTest : OuterClassGenTest() {
    override val backend: TargetBackend
        get() = JVM_IR

    // Class lambda not generated

    override fun testLambdaInConstructor() {
    }

    override fun testLambdaInInlineFunction() {
    }

    override fun testLambdaInInlineLambda() {
    }

    override fun testLambdaInLambdaInlinedIntoObject() {
    }

    override fun testLambdaInLambdaInlinedIntoObject2() {
    }

    override fun testLambdaInNoInlineFun() {
    }

    override fun testLambdaInlined() {
    }

    override fun testLocalObjectInInlineLambda() {
    }

    override fun testLocalObjectInLambdaInlinedIntoObject2() {
    }
}

open class IrPropertyGenTest : PropertyGenTest() {
    override val backend: TargetBackend
        get() = JVM_IR
}

open class IrKotlinSyntheticClassAnnotationTest : KotlinSyntheticClassAnnotationTest() {
    override val backend: TargetBackend
        get() = JVM_IR

    override fun updateConfiguration(configuration: CompilerConfiguration) {
        // Current tests rely on class lambdas
        configuration.put(JVMConfigurationKeys.LAMBDAS, JvmClosureGenerationScheme.CLASS)
        configuration.put(JVMConfigurationKeys.SAM_CONVERSIONS, JvmClosureGenerationScheme.CLASS)
        super.updateConfiguration(configuration)
    }

    override fun testLocalFunction() {
        // Indy is generated, irrelevant test
    }
}

open class IrVarArgTest : VarArgTest() {
    override val backend: TargetBackend
        get() = JVM_IR
}

open class IrControlStructuresTest : ControlStructuresTest() {
    override val backend: TargetBackend
        get() = JVM_IR

    override fun testCompareToNonnullableNotEq() {
        // https://youtrack.jetbrains.com/issue/KT-65357
    }
}

open class IrInnerClassInfoGenTest : InnerClassInfoGenTest() {
    override val backend: TargetBackend
        get() = JVM_IR

    // Test is irrelevant with indy lambdas.

    override fun testLambdaClassFlags() {
    }
}

open class IrReflectionClassLoaderTest : ReflectionClassLoaderTest() {
    override val backend: TargetBackend
        get() = JVM_IR
}

open class IrSourceInfoGenTest : SourceInfoGenTest() {
    override val backend: TargetBackend
        get() = JVM_IR
}
