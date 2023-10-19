// MODULE: m1-common
// FILE: common.kt

<!INCOMPATIBLE_MATCHING{JVM}("A; A; some expected members have no actual ones")!>expect annotation class A<!INCOMPATIBLE_MATCHING{JVM}("<init>; <init>; some value parameter is vararg in one declaration and non-vararg in the other")!>(vararg val x: String)<!><!>

@A("abc", "foo", "bar")
fun test() {}

// MODULE: m1-jvm()()(m1-common)
// FILE: jvm.kt

actual annotation class A(val x: Array<String>)

@A(<!ARGUMENT_TYPE_MISMATCH!>"abc"<!>, <!TOO_MANY_ARGUMENTS!>"foo"<!>, <!TOO_MANY_ARGUMENTS!>"bar"<!>)
fun test2() {}
