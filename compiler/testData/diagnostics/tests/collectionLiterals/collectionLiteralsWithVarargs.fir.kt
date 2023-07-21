// !LANGUAGE: +ProhibitAssigningSingleElementsToVarargsInNamedForm

annotation class Ann1(vararg val a: String = [])
annotation class Ann2(vararg val a: Int = [1, 2])
annotation class Ann3(vararg val a: Float = [1f])
annotation class Ann4(vararg val a: String = ["/"])

annotation class Ann5(vararg val a: Ann4 = [])
annotation class Ann6(vararg val a: Ann4 = [Ann4(*["a", "b"])])

annotation class Ann7(vararg val a: Long = [1L, <!NULL_FOR_NONNULL_TYPE!>null<!>, <!ARGUMENT_TYPE_MISMATCH!>""<!>])

@Ann1(*[])
fun test1_0() {}

@Ann1(*["a", "b"])
fun test1_1() {}

@Ann1(*<!ARGUMENT_TYPE_MISMATCH!>["a", 1, null]<!>)
fun test1_2() {}

@Ann2(*[])
fun test2() {}

@Ann3(a = *<!REDUNDANT_SPREAD_OPERATOR_IN_NAMED_FORM_IN_ANNOTATION!>[0f, <!DIVISION_BY_ZERO!>1 / 0f<!>]<!>)
fun test3() {}

@Ann5(Ann4(*["/"]))
fun test5() {}

@Ann6(*[])
fun test6() {}

@Ann7(1, 2)
fun test7() {}

annotation class AnnArray(val a: Array<String>)

@AnnArray(<!NON_VARARG_SPREAD!>*<!>["/"])
fun testArray() {}

@Ann1(<!ARGUMENT_TYPE_MISMATCH!>[""]<!>)
fun testVararg() {}
