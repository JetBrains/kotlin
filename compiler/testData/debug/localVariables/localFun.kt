//FILE: test.kt

fun foo() {
    fun bar()  {
    }
}

fun box() {
    foo()
}
// IGNORE_BACKEND: JVM_IR
// LOCAL VARIABLES
// test.kt:9 box:
// test.kt:4 foo:
// test.kt:6 foo: $fun$bar$1:TestKt$foo$1=TestKt$foo$1
// test.kt:10 box: