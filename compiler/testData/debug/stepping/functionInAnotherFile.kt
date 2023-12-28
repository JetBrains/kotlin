
// FILE: foo.kt
import bar
fun foo(x: Int): Int {
    if (x >= 0) {   // 5
        return x    // 6
    }
    return bar(x)   // 8
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

// EXPECTATIONS JVM_IR
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

// EXPECTATIONS WASM
// test.kt:1 $box
// test.kt:14 $box (8, 4, 4)
// foo.kt:5 $foo (8, 13, 8, 8, 13, 8)
// foo.kt:8 $foo (15, 11, 4)
// test.kt:18 $bar (8, 12, 8)
// test.kt:19 $bar (12, 8)
// foo.kt:6 $foo (15, 8)
// test.kt:22 $bar
// test.kt:15 $box
