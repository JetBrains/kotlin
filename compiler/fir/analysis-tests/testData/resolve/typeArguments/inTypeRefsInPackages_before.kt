// ISSUE: KT-84167
// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: -ForbidUselessTypeArgumentsIn25

// FILE: part1/part2/part3/tests.kt

package part1.part2.part3

interface I
open class O

class C : part1.part2<!TYPE_ARGUMENTS_NOT_ALLOWED_WARNING!><Int><!>.part3.O(), part1<!TYPE_ARGUMENTS_NOT_ALLOWED_WARNING!><Int><!>.part2.part3.I {
    val memberProperty: part1.part2<!TYPE_ARGUMENTS_NOT_ALLOWED_WARNING!><Int><!>.part3.C get() = this
}

typealias T = part1.part2<!TYPE_ARGUMENTS_NOT_ALLOWED_WARNING!><Int><!>.part3.I

fun testLocal() {
    val e3: part1.part2.part3<!TYPE_ARGUMENTS_NOT_ALLOWED!><Int><!>.C = C()
    val e2: part1.part2<!TYPE_ARGUMENTS_NOT_ALLOWED_WARNING!><Int><!>.part3.C = C()
    val e1: part1<!TYPE_ARGUMENTS_NOT_ALLOWED_WARNING!><Int><!>.part2.part3.C = C()

    val e: part1<!TYPE_ARGUMENTS_NOT_ALLOWED_WARNING!><<!UNRESOLVED_REFERENCE!>Unresolved<!>><!>.part2.part3.I
}

fun topLevelFunction(): part1.part2<!TYPE_ARGUMENTS_NOT_ALLOWED_WARNING!><Int><!>.part3.C = C()
val topLevelProperty: part1<!TYPE_ARGUMENTS_NOT_ALLOWED_WARNING!><Int><!>.part2.part3.C
    get() = C()

fun testCastAndIsInstance(x: Any, y: part1.part2<!TYPE_ARGUMENTS_NOT_ALLOWED_WARNING!><Int><!>.part3.O) {
    y is part1.part2<!TYPE_ARGUMENTS_NOT_ALLOWED_WARNING!><Int><!>.part3.I
    x as part1<!TYPE_ARGUMENTS_NOT_ALLOWED_WARNING!><Int><!>.part2.part3.T
}

fun testTypeArgs() {
    class Box<T>
    val y: Box<part1<!TYPE_ARGUMENTS_NOT_ALLOWED_WARNING!><Int><!>.part2.part3.C> = Box()
    Box<part1.part2<!TYPE_ARGUMENTS_NOT_ALLOWED_WARNING!><Int><!>.part3.C>()

    val functional: (part1<!TYPE_ARGUMENTS_NOT_ALLOWED_WARNING!><Int><!>.part2.part3.C) -> Unit = { }
}

/* GENERATED_FIR_TAGS: asExpression, classDeclaration, functionDeclaration, getter, interfaceDeclaration, isExpression,
localProperty, propertyDeclaration, thisExpression, typeAliasDeclaration */
