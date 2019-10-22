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
// TestKt:9:
// TestKt:4: $fun$bar$1:TestKt$foo$1
// TestKt:6: $fun$bar$1:TestKt$foo$1
// TestKt:10: