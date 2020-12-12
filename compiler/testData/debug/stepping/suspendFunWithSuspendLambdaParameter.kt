// WITH_COROUTINES
// FILE: test.kt
suspend fun foo(block: suspend Long.() -> String): String {
    return 1L.block()
}

suspend fun box() {
    foo {
        "OK"
    }
}

// LINENUMBERS
// test.kt:8 box
// test.kt:4 foo
// CoroutineUtil.kt:28 getContext
// test.kt:-1 <init>
// test.kt:-1 create
// test.kt:-1 invoke
// test.kt:8 invokeSuspend
// test.kt:9 invokeSuspend
// test.kt:-1 invoke
// test.kt:4 foo
// test.kt:8 box
// test.kt:11 box