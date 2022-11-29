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
// test.kt:13 box
// foo.kt:4 foo
// foo.kt:7 foo
// test.kt:17 bar
// test.kt:18 bar
// foo.kt:4 foo
// foo.kt:5 foo
// test.kt:18 bar
// test.kt:21 bar
// foo.kt:7 foo
// test.kt:13 box
// test.kt:14 box

// EXPECTATIONS JS_IR
// test.kt:13 box
// foo.kt:4 foo
// foo.kt:7 foo
// test.kt:17 bar
// test.kt:18 bar
// foo.kt:4 foo
// foo.kt:5 foo
// test.kt:21 bar
// test.kt:14 box