// IGNORE_BACKEND_K2_LIGHT_TREE: JVM_IR
//   Reason: KT-56755
// FILE: test.kt

fun foo(x: Any) {
    when (x) {
        is Float ->
            1
        is Double ->
            2
        else ->
            3
    }
}

fun box() {
    foo(1.2f)
    foo(1.2)
    foo(1)
}

// EXPECTATIONS JVM JVM_IR
// test.kt:17 box
// test.kt:6 foo
// test.kt:7 foo
// test.kt:8 foo
// test.kt:14 foo
// test.kt:18 box
// test.kt:6 foo
// test.kt:7 foo
// test.kt:9 foo
// test.kt:10 foo
// test.kt:14 foo
// test.kt:19 box
// test.kt:6 foo
// test.kt:7 foo
// test.kt:9 foo
// test.kt:12 foo
// test.kt:14 foo
// test.kt:20 box

// EXPECTATIONS JS_IR
// test.kt:17 box
// test.kt:6 foo
// test.kt:7 foo
// test.kt:14 foo
// test.kt:18 box
// test.kt:6 foo
// test.kt:7 foo
// test.kt:14 foo
// test.kt:19 box
// test.kt:6 foo
// test.kt:7 foo
// test.kt:14 foo
// test.kt:20 box
