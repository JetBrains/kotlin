
// FILE: test.kt
fun foo(block: Long.() -> String): String {
    return 1L.block()
}

fun box() {
    foo {
        "OK"
    }
}

// EXPECTATIONS ClassicFrontend JVM_IR
// test.kt:8 box:
// test.kt:4 foo: block:kotlin.jvm.functions.Function1=TestKt$box$1
// test.kt:9 invoke: $this$foo:long=1:long
// test.kt:4 foo: block:kotlin.jvm.functions.Function1=TestKt$box$1
// test.kt:8 box:
// test.kt:11 box:

// EXPECTATIONS FIR JVM_IR
// test.kt:8 box:
// test.kt:4 foo: block:kotlin.jvm.functions.Function1=TestKt$<lambda>
// test.kt:9 box$lambda$0: $this$foo:long=1:long
// test.kt:4 foo: block:kotlin.jvm.functions.Function1=TestKt$<lambda>
// test.kt:8 box:
// test.kt:11 box:

// EXPECTATIONS JS_IR
// test.kt:8 box:
// test.kt:4 foo: block=Function1
// test.kt:4 foo: block=Function1
// test.kt:9 box$lambda: $this$foo=kotlin.Long
// test.kt:11 box:
