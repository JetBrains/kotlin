@Retention(AnnotationRetention.RUNTIME)
annotation class Anno(vararg val x: String, val y: String)

@Anno(x = <!ARGUMENT_TYPE_MISMATCH!>[["a", "b"], ["a", "b"]]<!>, y = "a")
fun foo1() {}

@Anno(x = <!ARGUMENT_TYPE_MISMATCH!>[[["a"]]]<!>, y = "b")
fun foo11() {}

@Anno(x = ["a", "b"], y = "a")
fun foo2() {}

@Anno(x = <!ARGUMENT_TYPE_MISMATCH!>arrayOf(arrayOf("a"), arrayOf("b"))<!>, y = "a")
fun foo3() {}

@Anno(x = arrayOf("a", "b"), y = "a")
fun foo4() {}

@Retention(AnnotationRetention.RUNTIME)
annotation class Anno1(val x: Array<in String>, val y: String)

@Retention(AnnotationRetention.RUNTIME)
annotation class Anno2(vararg val x: String, val y: String)

@Anno1(x = ["", Anno2(x = [""], y = "")], y = "")
fun foo5() {}
