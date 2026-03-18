// ISSUE: KT-84167
// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE: -ForbidUselessTypeArgumentsIn25

// FILE: part1/part2/part3/test.kt

package part1.part2.part3

class C {
    fun M() { }
}

fun test() {
    val c: part1.part2<!TYPE_ARGUMENTS_NOT_ALLOWED_WARNING!><Int><!>.part3.C = C()
    if (<!USELESS_IS_CHECK!>c is part1<!TYPE_ARGUMENTS_NOT_ALLOWED_WARNING!><Int><!>.part2.part3.C<!>) {
        c.M()
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, ifExpression, isExpression, localProperty,
propertyDeclaration */
