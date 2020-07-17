// FILE: foo.kt
import bar
fun foo(x: Int): Int {
    if (x >= 0) {   // 4
        return x    // 5
    }
    return bar(x)   // 7
}

//FILE: test.kt
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

// LINENUMBERS
// test.kt:4 box
// foo.kt:4 foo
// foo.kt:7 foo
// test.kt:8 bar
// test.kt:9 bar
// foo.kt:4 foo
// foo.kt:5 foo
// test.kt:9 bar
// test.kt:12 bar
// foo.kt:7 foo
// test.kt:4 box
// test.kt:5 box
