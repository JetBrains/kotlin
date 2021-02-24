// !WITH_NEW_INFERENCE
// !LANGUAGE: +ArrayLiteralsInAnnotations, +ProhibitAssigningSingleElementsToVarargsInNamedForm

annotation class Ann1(vararg val a: String = [])
annotation class Ann2(vararg val a: Int = [1, 2])
annotation class Ann3(vararg val a: Float = [1f])
annotation class Ann4(vararg val a: String = ["/"])

annotation class Ann5(vararg val a: Ann4 = [])
annotation class Ann6(vararg val a: Ann4 = [Ann4(*["a", "b"])])

annotation class Ann7(vararg val a: Long = [1L, null, ""])

@Ann1(*[])
fun test1_0() {}

@Ann1(*["a", "b"])
fun test1_1() {}

@Ann1(*["a", 1, null])
fun test1_2() {}

@Ann2(*[])
fun test2() {}

@Ann3(a = *[0f, 1 / 0f])
fun test3() {}

@Ann5(Ann4(*["/"]))
fun test5() {}

@Ann6(*[])
fun test6() {}

annotation class AnnArray(val a: Array<String>)

<!INAPPLICABLE_CANDIDATE!>@AnnArray(*["/"])<!>
fun testArray() {}

@Ann1([""])
fun testVararg() {}
