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

<!INAPPLICABLE_CANDIDATE!>@Ann(s = intArrayOf())<!>
fun test2() {}

<!INAPPLICABLE_CANDIDATE!>@Ann(s = arrayOf(1))<!>
fun test3() {}

@Ann("value1", "value2")
fun test4() {}

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
