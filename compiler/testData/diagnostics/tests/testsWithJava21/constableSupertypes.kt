// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-29858

import java.lang.constant.Constable
import java.lang.constant.ConstantDesc

enum class MyEnum { ENTRY }

fun takeConstable(c: Constable) {}
fun takeConstantDesc(d: ConstantDesc) {}

fun testConstable(b: Byte, s: Short, i: Int, l: Long, f: Float, d: Double, c: Char, z: Boolean, str: String, e: MyEnum) {
    takeConstable(<!ARGUMENT_TYPE_MISMATCH!>b<!>)
    takeConstable(<!ARGUMENT_TYPE_MISMATCH!>s<!>)
    takeConstable(<!ARGUMENT_TYPE_MISMATCH!>i<!>)
    takeConstable(<!ARGUMENT_TYPE_MISMATCH!>l<!>)
    takeConstable(<!ARGUMENT_TYPE_MISMATCH!>f<!>)
    takeConstable(<!ARGUMENT_TYPE_MISMATCH!>d<!>)
    takeConstable(<!ARGUMENT_TYPE_MISMATCH!>c<!>)
    takeConstable(<!ARGUMENT_TYPE_MISMATCH!>z<!>)
    takeConstable(<!ARGUMENT_TYPE_MISMATCH!>str<!>)
    takeConstable(<!ARGUMENT_TYPE_MISMATCH!>e<!>)
}

fun testConstantDesc(b: Byte, s: Short, i: Int, l: Long, f: Float, d: Double, c: Char, z: Boolean, str: String, e: MyEnum) {
    takeConstantDesc(<!ARGUMENT_TYPE_MISMATCH!>i<!>)
    takeConstantDesc(<!ARGUMENT_TYPE_MISMATCH!>l<!>)
    takeConstantDesc(<!ARGUMENT_TYPE_MISMATCH!>f<!>)
    takeConstantDesc(<!ARGUMENT_TYPE_MISMATCH!>d<!>)
    takeConstantDesc(<!ARGUMENT_TYPE_MISMATCH!>str<!>)

    // The Java analogues of these types implement only Constable, not ConstantDesc,
    // so the following calls must be errors even after KT-29858
    takeConstantDesc(<!ARGUMENT_TYPE_MISMATCH!>b<!>)
    takeConstantDesc(<!ARGUMENT_TYPE_MISMATCH!>s<!>)
    takeConstantDesc(<!ARGUMENT_TYPE_MISMATCH!>c<!>)
    takeConstantDesc(<!ARGUMENT_TYPE_MISMATCH!>z<!>)
    takeConstantDesc(<!ARGUMENT_TYPE_MISMATCH!>e<!>)
}

fun testFromIssue() {
    var foo: Constable <!INITIALIZER_TYPE_MISMATCH!>=<!> 1
    foo <!ASSIGNMENT_TYPE_MISMATCH!>=<!> "str"
}

/* GENERATED_FIR_TAGS: assignment, enumDeclaration, enumEntry, functionDeclaration, integerLiteral, localProperty,
propertyDeclaration, stringLiteral */
