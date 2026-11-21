// FILE: test.kt

enum class E {
    X,

    Y {
        fun foo() = 23
    },

    Z() {
        fun bar() = 42
    }
}

fun box() {
    E.X;
    E.Y;
    E.Z;
}

// EXPECTATIONS JVM
// test.kt:16 box
// test.kt:4 <clinit>
// test.kt:6 <clinit>
// test.kt:10 <clinit>
// test.kt:17 box
// test.kt:18 box
// test.kt:19 box

// EXPECTATIONS NATIVE
// test.kt:16 box
// test.kt:3 $getEnumAt
// test.kt:13 $getEnumAt
// test.kt:16 box
// test.kt:17 box
// test.kt:3 $getEnumAt
// test.kt:13 $getEnumAt
// test.kt:17 box
// test.kt:18 box
// test.kt:3 $getEnumAt
// test.kt:13 $getEnumAt
// test.kt:19 box

// EXPECTATIONS JS_IR JS_IR_ES6
// test.kt:16 box
// test.kt:4 E$static_init
// test.kt:6 E$static_init
// test.kt:10 E$static_init
// test.kt:17 box
// test.kt:18 box
// test.kt:19 box

// EXPECTATIONS WASM_JS
// test.kt:16 $box (6)
// test.kt:4 $E$static_init (4)
// test.kt:6 $E$static_init (4)
// test.kt:10 $E$static_init (4)
// test.kt:16 $box (6)
// test.kt:17 $box (6)
// test.kt:4 $E$static_init (4)
// test.kt:17 $box (6)
// test.kt:18 $box (6)
// test.kt:4 $E$static_init (4)
// test.kt:18 $box (6)
// test.kt:19 $box (1)
