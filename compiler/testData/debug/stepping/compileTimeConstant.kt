// FILE: test.kt

fun box() {
    val x =
            42
}

// IGNORE_BACKEND: JVM_IR
// The JVM_IR backend does not hit line 4. That should be fixed.

// LINENUMBERS
// test.kt:5 box
// test.kt:4 box
// test.kt:6 box
