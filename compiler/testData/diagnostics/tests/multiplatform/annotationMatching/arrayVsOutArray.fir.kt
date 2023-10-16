// MODULE: m1-common
// FILE: common.kt

expect annotation class A(val x: Array<out String>)

@A("abc", <!TOO_MANY_ARGUMENTS!>"foo"<!>, <!TOO_MANY_ARGUMENTS!>"bar"<!>)
fun test() {}

// MODULE: m1-jvm()()(m1-common)
// FILE: jvm.kt

actual annotation class A(val x: Array<String>)

@A("abc", <!TOO_MANY_ARGUMENTS!>"foo"<!>, <!TOO_MANY_ARGUMENTS!>"bar"<!>)
fun test2() {}

annotation class B(val x: Array<out String>)

@B("abc", <!TOO_MANY_ARGUMENTS!>"foo"<!>, <!TOO_MANY_ARGUMENTS!>"bar"<!>)
fun test3() {}
