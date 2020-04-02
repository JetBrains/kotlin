//FILE: foo.kt
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
// test.kt:4
// foo.kt:4
// foo.kt:7
// test.kt:8
// test.kt:9
// foo.kt:4
// foo.kt:5
// test.kt:9
// test.kt:12
// foo.kt:7
// test.kt:4
// test.kt:5
