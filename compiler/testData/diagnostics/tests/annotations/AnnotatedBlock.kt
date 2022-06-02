// FIR_IDENTICAL
// FIR_DUMP
// ISSUE: KT-52175

annotation class Ann
annotation class Ann2

fun test(x: String?) {
    if (x != null)
        @Ann() @Ann2() { Unit } // It should be Block with annotations

    if (x != null)
        @Ann() @Ann2() Unit // It should be SingleExpressionBlock  with annotations
}