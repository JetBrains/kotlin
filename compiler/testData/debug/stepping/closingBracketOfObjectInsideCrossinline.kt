// See KT-64727

// WITH_STDLIB
// FILE: test.kt

inline fun <reified T : Any> foo(crossinline function: () -> T) {


    object {
        fun bar() {
            function()
        }

        init {
            bar()
        }
    }
}

fun box() {
    var result = "Fail"
    foo {
        object {
            init {
                result = "OK"
            } // Should be hit
        }
    }
}

// EXPECTATIONS JVM_IR
// test.kt:21 box
// test.kt:22 box
// test.kt:9 box
// test.kt:9 <init>
// test.kt:14 <init>
// test.kt:15 <init>
// test.kt:11 bar
// test.kt:23 bar
// test.kt:23 <init>
// test.kt:24 <init>
// test.kt:25 <init>
// test.kt:26 <init>
// test.kt:23 <init>
// test.kt:27 bar
// test.kt:11 bar
// test.kt:12 bar
// test.kt:16 <init>
// test.kt:9 <init>
// test.kt:9 box
// test.kt:18 box
// test.kt:29 box

// EXPECTATIONS JS_IR
// test.kt:21 box
// test.kt:9 box
// test.kt:15 <init>
// test.kt:23 bar
// test.kt:25 <init>
// test.kt:23 <init>
// test.kt:12 bar
// test.kt:9 <init>
// test.kt:29 box

// EXPECTATIONS WASM
// test.kt:21 $box (17, 4)
// test.kt:22 $box
// test.kt:9 $box (4, 4)
// test.kt:15 $<no name provided>.<init> (12, 12)
// test.kt:11 $<no name provided>.bar
// test.kt:23 $<no name provided>.bar (8, 8, 8)
// test.kt:25 $<no name provided>.<init> (16, 25, 16)
// test.kt:27 $<no name provided>.<init>
// test.kt:27 $<no name provided>.bar
// test.kt:12 $<no name provided>.bar
// test.kt:17 $<no name provided>.<init>
// test.kt:29 $box
