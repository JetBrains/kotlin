
// FILE: test.kt
fun foo(block: Long.() -> String): String {
    return 1L.block()
}

fun box() {
    foo {
        "OK"
    }
}

// LOCAL VARIABLES
// test.kt:8 box:
// test.kt:4 foo: block:kotlin.jvm.functions.Function1=TestKt$box$1
// test.kt:9 invoke: $this$foo:long=1:long
// test.kt:4 foo: block:kotlin.jvm.functions.Function1=TestKt$box$1
// test.kt:8 box:
// test.kt:11 box: