// FIR_IDENTICAL
// MODULE: library
// KOTLINC_ARGS: -Xjvm-default=all
// FILE: a.kt
package base

interface UExpression {
    fun evaluate(): Any? = "fail"
}

// MODULE: main(library)
// KOTLINC_ARGS: -Xjvm-default=disable -XXLanguage:-AllowSuperCallToJavaInterface
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
