// FIR_IDENTICAL
// RENDER_IR_DIAGNOSTICS_FULL_TEXT
// LANGUAGE: +IntrinsicConstEvaluation
// TARGET_BACKEND: JVM_IR
// WITH_STDLIB

object A {
    const val recursive1: Int = <!CONST_VAL_WITH_NON_CONST_INITIALIZER, EVALUATION_ERROR!>1 + B.recursive2<!>
}

class B {
    companion object {
        const val recursive2: Int = <!CONST_VAL_WITH_NON_CONST_INITIALIZER, EVALUATION_ERROR!>A.recursive1 + 2<!>
    }
}
