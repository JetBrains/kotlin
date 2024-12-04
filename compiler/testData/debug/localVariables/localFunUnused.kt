

//FILE: test.kt
fun foo() {
    fun bar()  {
    }
}

fun box() {
    foo()
}

// EXPECTATIONS
// test.kt:10 box:
// test.kt:7 foo:
// test.kt:11 box:
