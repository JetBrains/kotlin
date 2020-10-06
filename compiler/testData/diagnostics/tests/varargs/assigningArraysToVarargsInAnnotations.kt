// !WITH_NEW_INFERENCE
// !LANGUAGE: +ArrayLiteralsInAnnotations, +AssigningArraysToVarargsInNamedFormInAnnotations

// FILE: JavaAnn.java

@interface JavaAnn {
    String[] value() default {};
    String[] path() default {};
}

// FILE: test.kt

annotation class Ann(vararg val s: String)

@Ann(s = arrayOf())
fun test1() {}

@Ann(s = <!TYPE_MISMATCH!>intArrayOf()<!>)
fun test2() {}

@Ann(s = <!NI;TYPE_MISMATCH, OI;TYPE_INFERENCE_EXPECTED_TYPE_MISMATCH!>arrayOf(1)<!>)
fun test3() {}

@Ann("value1", "value2")
fun test4() {}

@Ann(s = ["value"])
fun test5() {}

@JavaAnn(value = arrayOf("value"))
fun jTest1() {}

@JavaAnn(value = ["value"])
fun jTest2() {}

@JavaAnn(<!NI;CHANGING_ARGUMENTS_EXECUTION_ORDER_FOR_NAMED_VARARGS!>value = ["value"]<!>, path = ["path"])
fun jTest3() {}


annotation class IntAnn(vararg val i: Int)

@IntAnn(i = [1, 2])
fun foo1() {}

@IntAnn(i = intArrayOf(0))
fun foo2() {}