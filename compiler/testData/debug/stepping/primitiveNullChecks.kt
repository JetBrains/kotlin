// FILE: test.kt

fun box(): String {
    42!!
    42.toLong()!!
    return "OK"!!
}

// EXPECTATIONS
// test.kt:4 box
// test.kt:5 box
// test.kt:6 box