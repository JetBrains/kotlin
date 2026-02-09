// ISSUE: KT-84167
// RUN_PIPELINE_TILL: BACKEND

// FILE: part1/part2/part3/test.kt

package part1.part2.part3

class C {
    fun M() { }
}

fun test() {
    val c: part1.part2<Int>.part3.C = C()
    if (<!USELESS_IS_CHECK!>c is part1<Int>.part2.part3.C<!>) {
        c.M()
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, ifExpression, isExpression, localProperty,
propertyDeclaration */
