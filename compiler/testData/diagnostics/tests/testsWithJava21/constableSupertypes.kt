// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-29858

import java.lang.constant.Constable
import java.lang.constant.ConstantDesc

enum class MyEnum { ENTRY }

fun takeConstable(c: Constable) {}
fun takeConstantDesc(d: ConstantDesc) {}

fun testConstable(b: Byte, s: Short, i: Int, l: Long, f: Float, d: Double, c: Char, z: Boolean, str: String, e: MyEnum) {
    takeConstable(b)
    takeConstable(s)
    takeConstable(i)
    takeConstable(l)
    takeConstable(f)
    takeConstable(d)
    takeConstable(c)
    takeConstable(z)
    takeConstable(str)
    takeConstable(e)
}

fun testConstantDesc(b: Byte, s: Short, i: Int, l: Long, f: Float, d: Double, c: Char, z: Boolean, str: String, e: MyEnum) {
    takeConstantDesc(i)
    takeConstantDesc(l)
    takeConstantDesc(f)
    takeConstantDesc(d)
    takeConstantDesc(str)

    // The Java analogues of these types implement only Constable, not ConstantDesc,
    // so the following calls must be errors even after KT-29858
    takeConstantDesc(<!ARGUMENT_TYPE_MISMATCH!>b<!>)
    takeConstantDesc(<!ARGUMENT_TYPE_MISMATCH!>s<!>)
    takeConstantDesc(<!ARGUMENT_TYPE_MISMATCH!>c<!>)
    takeConstantDesc(<!ARGUMENT_TYPE_MISMATCH!>z<!>)
    takeConstantDesc(<!ARGUMENT_TYPE_MISMATCH!>e<!>)
}

fun testFromIssue() {
    var foo: Constable = 1
    foo = "str"
}

/* GENERATED_FIR_TAGS: assignment, enumDeclaration, enumEntry, functionDeclaration, integerLiteral, localProperty,
propertyDeclaration, stringLiteral */
