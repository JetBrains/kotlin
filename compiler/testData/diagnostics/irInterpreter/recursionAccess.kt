// FIR_IDENTICAL
// !RENDER_IR_DIAGNOSTICS_FULL_TEXT
// !LANGUAGE: +IntrinsicConstEvaluation
// TARGET_BACKEND: JVM_IR
// WITH_STDLIB

object A {
    const val recursive1: Int = 1 <!EVALUATION_ERROR!>+ B.recursive2<!>
}

class B {
    companion object {
        const val recursive2: Int = A.recursive1 <!EVALUATION_ERROR!>+ 2<!>
    }
}
