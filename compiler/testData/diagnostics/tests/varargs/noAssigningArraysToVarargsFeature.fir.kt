// !WITH_NEW_INFERENCE
// !LANGUAGE: +ArrayLiteralsInAnnotations, -AssigningArraysToVarargsInNamedFormInAnnotations -AllowAssigningArrayElementsToVarargsInNamedFormForFunctions

// FILE: JavaAnn.java

@interface JavaAnn {
    String[] value() default {};
    String[] path() default {};
}

// FILE: test.kt

annotation class Ann(vararg val s: String)

@Ann(s = arrayOf())
fun test1() {}

@Ann(s = intArrayOf())
fun test2() {}

@Ann(s = arrayOf(1))
fun test3() {}

@Ann(s = ["value"])
fun test5() {}

@JavaAnn(value = arrayOf("value"))
fun jTest1() {}

@JavaAnn(value = ["value"])
fun jTest2() {}

@JavaAnn(value = ["value"], path = ["path"])
fun jTest3() {}

annotation class IntAnn(vararg val i: Int)

@IntAnn(i = [1, 2])
fun foo1() {}

@IntAnn(i = intArrayOf(0))
fun foo2() {}

fun foo(vararg i: Int) {}

@Ann(s = "value")
fun dep1() {
    <!INAPPLICABLE_CANDIDATE!>foo<!>(i = 1)
}