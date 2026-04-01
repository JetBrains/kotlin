
// FILE: test.kt

@Target(AnnotationTarget.EXPRESSION,
        AnnotationTarget.VALUE_PARAMETER,
        AnnotationTarget.TYPE_PARAMETER)
@Retention(AnnotationRetention.SOURCE)
annotation class Anno

fun <@Anno T>
        foo(@Anno x: T): String {
    return (@Anno "OK")
}

fun box(): String =
    foo(42)

// EXPECTATIONS JVM_IR
// test.kt:16 box
// test.kt:12 foo
// test.kt:16 box

// EXPECTATIONS NATIVE
// test.kt:16 box
// test.kt:10 foo
// test.kt:12 foo
// test.kt:13 foo
// test.kt:16 box

// EXPECTATIONS JS_IR
// test.kt:16 box
// test.kt:12 foo

// EXPECTATIONS WASM
// test.kt:16 $box (8, 4)
// test.kt:12 $foo (18, 4)
// test.kt:16 $box (11)
