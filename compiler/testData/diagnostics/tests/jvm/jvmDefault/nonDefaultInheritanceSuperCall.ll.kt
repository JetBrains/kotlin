// LL_FIR_DIVERGENCE
// LL analyzes `library` from sources, and `isCompiledToJvmDefault` uses the JVM default mode of the use-site module
// for source declarations instead of the binary flag set by `JVM_DEFAULT_MODE: no-compatibility` of `library`.
// LL_FIR_DIVERGENCE
// RUN_PIPELINE_TILL: FRONTEND
// MODULE: library
// JVM_DEFAULT_MODE: no-compatibility
// FILE: a.kt
package base

interface UExpression {
    fun evaluate(): Any? = "fail"
}

// MODULE: main(library)
// JVM_DEFAULT_MODE: disable
// LANGUAGE: -AllowSuperCallToJavaInterface
// FILE: source.kt
import base.*

interface KotlinInterface : UExpression {
    override fun evaluate(): Any? {
        return super.evaluate()
    }
}

class KotlinClass : UExpression {
    override fun evaluate(): Any? {
        return super.evaluate()
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, interfaceDeclaration, nullableType, override,
stringLiteral, superExpression */
