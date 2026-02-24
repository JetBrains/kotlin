// FILE: test.kt

fun foo(block: (String) -> String) = block("")

fun box(): String {
    return foo {
        fun localFun(): (String) -> String {
            return { p: String -> p}
        }
        localFun()("OK")
    }
}

// EXPECTATIONS FIR JVM_IR

// test.kt:6 box
// test.kt:3 foo
// test.kt:10 box$lambda$0
// test.kt:8 box$lambda$0$localFun
// test.kt:10 box$lambda$0
// test.kt:8 box$lambda$0$localFun$0
// test.kt:10 box$lambda$0
// test.kt:3 foo
// test.kt:6 box


// EXPECTATIONS ClassicFrontend JVM_IR
// test.kt:6 box
// test.kt:3 foo
// test.kt:10 invoke
// test.kt:8 invoke$localFun
// test.kt:10 invoke
// test.kt:8 invoke
// test.kt:10 invoke
// test.kt:3 foo
// test.kt:6 box

// EXPECTATIONS WASM
// test.kt:6 $box (11)
// test.kt:3 $foo (37, 43, 37)
// test.kt:10 $box$lambda.invoke (8)
// test.kt:8 $invoke$localFun (12)
// test.kt:10 $box$lambda.invoke (8, 19, 8)
// test.kt:8 $box$lambda$localFun$lambda.invoke (34, 35)
// test.kt:10 $box$lambda.invoke (8, 24)
// test.kt:3 $foo (37, 46)

// test.kt:6 $box (4)

// EXPECTATIONS JS_IR
// test.kt:6 box
// test.kt:3 foo
// test.kt:10 box$lambda
// test.kt:8 invoke$localFun
// test.kt:10 box$lambda
// test.kt:8 box$lambda$localFun$lambda

// EXPECTATIONS NATIVE
// test.kt:6 box
// test.kt:3 foo
// test.kt:6 invoke
// test.kt:10 invoke
// test.kt:7 invoke$localFun
// test.kt:8 invoke$localFun
// test.kt:9 invoke$localFun
// test.kt:10 invoke
// test.kt:8 invoke
// test.kt:10 invoke
// test.kt:11 invoke
// test.kt:3 foo
// test.kt:6 box
// test.kt:12 box
