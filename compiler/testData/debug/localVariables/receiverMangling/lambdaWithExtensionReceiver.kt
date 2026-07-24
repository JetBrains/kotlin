
// FILE: test.kt
fun foo(block: Long.() -> String): String {
    return 1L.block()
}

fun box() {
    foo {
        "OK"
    }
}

// EXPECTATIONS JVM_IR
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

// TODO(KT-86460): the FQN of `Function1` sometimes is `Function1` and sometimes `kotlin.test.Function1`, so specifying a correct option isn't possible here; re-enable this EXPECTATIONS WASM block after KT-86460 is solved by replacing this line with // EXPECTATIONS WASM
// test.kt:8 $box: (4)
// test.kt:4 $foo: $block:(ref $box$lambda)=(ref $box$lambda) (14, 11, 11, 11, 11, 11, 11, 14, 14, 14, 14, 14, 14, 14, 14, 14)
// test.kt:9 $box$lambda.invoke: $<this>:(ref $box$lambda)=(ref $box$lambda), $$this$foo:i64=i64 (8, 8, 8, 12)
// test.kt:4 $foo: $block:(ref $box$lambda)=(ref $box$lambda) (14, 14, 14, 14, 14, 14, 14, 14, 14, 4)
// test.kt:8 $box: (4)
// test.kt:11 $box: (1)
