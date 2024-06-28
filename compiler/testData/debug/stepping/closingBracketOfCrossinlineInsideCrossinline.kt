// See KT-64726
// IGNORE_INLINER: IR
// IGNORE_BACKEND: WASM
// FILE: test.kt
inline fun String.switchMapOnce(crossinline mapper: (String) -> String): String {
    return { mapper(this) }.let { it() }
}

fun box() {
    "O".switchMapOnce {

        "K".switchMapOnce {
            "OK"
        } // Should be present
    }
}

// EXPECTATIONS JVM_IR
// test.kt:10 box
// test.kt:6 box
// fake.kt:1 box
// test.kt:6 box
// test.kt:6 invoke
// test.kt:12 invoke
// test.kt:6 invoke
// fake.kt:1 invoke
// test.kt:6 invoke
// test.kt:6 invoke
// test.kt:13 invoke
// test.kt:6 invoke
// test.kt:6 invoke
// test.kt:14 invoke
// test.kt:6 invoke
// test.kt:6 box
// test.kt:16 box

// EXPECTATIONS JS_IR
// test.kt:6 box$lambda
// test.kt:6 box
// test.kt:6 box$lambda$lambda
// test.kt:6 box$lambda$lambda
// test.kt:6 box$lambda$lambda
// test.kt:6 box$lambda$lambda$lambda
// test.kt:16 box

// EXPECTATIONS WASM
// test.kt:10 $box (8, 4, 4, 4, 4, 8)
// test.kt:16 $box
