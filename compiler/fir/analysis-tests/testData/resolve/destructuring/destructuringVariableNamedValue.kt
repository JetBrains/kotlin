// ISSUE: KT-83920
// WITH_STDLIB
// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +NameBasedDestructuring

data class Box(val key: String, val value: String)

fun foo1(b: Box, l: List<Box>) {
    val [key, <!WRONG_MODIFIER_TARGET!>value<!><!SYNTAX!>]<!><!SYNTAX!><!> = b
    for ([key, <!WRONG_MODIFIER_TARGET!>value<!><!SYNTAX!>]<!><!SYNTAX!><!> in l) {}
    l.forEach { [key, <!WRONG_MODIFIER_TARGET!>value<!><!SYNTAX!>]<!><!SYNTAX!><!> -> }
}
fun foo2(b: Box, l: List<Box>) {
    [val key, val value] = b
    for ([val key, val value] in l) {}
    l.forEach { [val key, val value] -> }
}
fun foo3(b: Box, l: List<Box>) {
    val (key, value) = b
    for ((key, value) in l) {}
    l.forEach { (key, value) -> }
}
fun foo4(b: Box, l: List<Box>) {
    (val key, val value) = b
    for ((val key, val value) in l) {}
    l.forEach { (val key, val value) -> }
}

/* GENERATED_FIR_TAGS: forLoop, functionDeclaration, localProperty, propertyDeclaration */

