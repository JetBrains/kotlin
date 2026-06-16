// FILE: test.kt

fun example(str: String) {}

enum class E {
    X,
    Y,
    Z {
        init {
            example("Z")
        }
    }
}

fun box() {
    E.X;
    E.Y;
    E.Z;
}

// EXPECTATIONS JVM_IR
// test.kt:16 box
// test.kt:6 <clinit>
// test.kt:7 <clinit>
// test.kt:8 <clinit>
// test.kt:17 box
// test.kt:18 box
// test.kt:19 box

// EXPECTATIONS NATIVE
// test.kt:16 box
// test.kt:5 $getEnumAt
// test.kt:13 $getEnumAt
// test.kt:16 box
// test.kt:17 box
// test.kt:5 $getEnumAt
// test.kt:13 $getEnumAt
// test.kt:17 box
// test.kt:18 box
// test.kt:5 $getEnumAt
// test.kt:13 $getEnumAt
// test.kt:19 box

// EXPECTATIONS JS_IR
// test.kt:16 box
// test.kt:6 E$static_init
// test.kt:5 <init>
// test.kt:7 E$static_init
// test.kt:5 <init>
// test.kt:8 E$static_init
// test.kt:8 <init>
// test.kt:5 <init>
// test.kt:10 <init>
// test.kt:3 example
// test.kt:8 <init>
// test.kt:17 box
// test.kt:18 box
// test.kt:19 box

// EXPECTATIONS WASM
// test.kt:16 $box (6)
// test.kt:6 $E$static_init (4)
// test.kt:13 $E.<init> (1)
// test.kt:6 $E$static_init (4)
// test.kt:7 $E$static_init (4)
// test.kt:13 $E.<init> (1)
// test.kt:7 $E$static_init (4)
// test.kt:8 $E$static_init (4)
// test.kt:8 $E$Z.<init> (4)
// test.kt:13 $E.<init> (1)
// test.kt:8 $E$Z.<init> (4)
// test.kt:10 $E$Z.<init> (20, 12)
// test.kt:3 $example (27)
// test.kt:12 $E$Z.<init> (5)
// test.kt:8 $E$static_init (4)
// test.kt:16 $box (6)
// test.kt:17 $box (6)
// test.kt:6 $E$static_init (4)
// test.kt:17 $box (6)
// test.kt:18 $box (6)
// test.kt:6 $E$static_init (4)
// test.kt:18 $box (6)
// test.kt:19 $box (1)
