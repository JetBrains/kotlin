// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL
// MODULE: library
// KOTLINC_ARGS: -jvm-default=no-compatibility
// JVM_DEFAULT_MODE: no-compatibility
// FILE: a.kt
package base

interface UExpression {
    fun evaluate(): Any? = "fail"
}

// MODULE: main(library)
// KOTLINC_ARGS: -jvm-default=disable -XXLanguage:-AllowSuperCallToJavaInterface
// JVM_DEFAULT_MODE: disable
// LANGUAGE: -AllowSuperCallToJavaInterface
// FILE: source.kt
import base.*

interface KotlinInterface : UExpression {
    override fun evaluate(): Any? {
        return super.<!INTERFACE_CANT_CALL_DEFAULT_METHOD_VIA_SUPER!>evaluate<!>()
    }
}

class KotlinClass : UExpression {
    override fun evaluate(): Any? {
        return super.evaluate()
    }
}
