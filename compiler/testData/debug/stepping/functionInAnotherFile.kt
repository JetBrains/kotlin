// IGNORE_BACKEND: WASM
// FILE: foo.kt
import bar
fun foo(x: Int): Int {
    if (x >= 0) {   // 4
        return x    // 5
    }
    return bar(x)   // 7
}

// FILE: test.kt
import foo
fun box() {
    foo(-3)            //4
}

fun bar(x: Int) =
    if (x < 0) {           //8
        foo(0)
    } else {               // 10
        foo(x)
    }

// EXPECTATIONS JVM JVM_IR
// test.kt:14 box
// foo.kt:5 foo
// foo.kt:8 foo
// test.kt:18 bar
// test.kt:19 bar
// foo.kt:5 foo
// foo.kt:6 foo
// test.kt:19 bar
// test.kt:22 bar
// foo.kt:8 foo
// test.kt:14 box
// test.kt:15 box

// EXPECTATIONS JS_IR
// test.kt:14 box
// foo.kt:5 foo
// foo.kt:8 foo
// test.kt:18 bar
// test.kt:19 bar
// foo.kt:5 foo
// foo.kt:6 foo
// test.kt:22 bar
// test.kt:15 box