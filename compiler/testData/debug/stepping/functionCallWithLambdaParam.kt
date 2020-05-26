// FILE: test.kt

fun box() {
    foo({
            val a = 1
        })

    foo() {
        val a = 1
    }
}

fun foo(f: () -> Unit) {
    f()
}

// LINENUMBERS
// test.kt:4 box
// test.kt:14 foo
// test.kt:5 invoke
// test.kt:6 invoke
// test.kt:14 foo
// test.kt:15 foo
// test.kt:8 box
// test.kt:14 foo
// test.kt:9 invoke
// test.kt:10 invoke
// test.kt:14 foo
// test.kt:15 foo
// test.kt:11 box
