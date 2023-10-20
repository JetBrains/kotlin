// MODULE: m1-common
// FILE: common.kt

expect annotation class A(vararg val x: String)

@A("abc", "foo", "bar")
fun test() {}

// MODULE: m1-jvm()()(m1-common)
// FILE: jvm.kt

actual annotation class A(val x: Array<String>)

@A(<!ARGUMENT_TYPE_MISMATCH!>"abc"<!>, <!TOO_MANY_ARGUMENTS!>"foo"<!>, <!TOO_MANY_ARGUMENTS!>"bar"<!>)
fun test2() {}
