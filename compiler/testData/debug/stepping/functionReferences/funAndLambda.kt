// FILE: test.kt

fun foo(block: (String) -> String) = block("")

fun box(): String {

    foo { foo { it } }

    foo {
        foo {
            it
        }
    }

    foo {
        it: String ->
        foo {
            innerIt: String ->
            innerIt
        }
    }

    foo()
    {
        it: String ->
        foo()
        {
            it: String ->
            it
        }
    }

    return "OK"
}

// EXPECTATIONS FIR JVM_IR

// test.kt:7 box
// test.kt:3 foo
// test.kt:7 box$lambda$0
// test.kt:3 foo
// test.kt:7 box$lambda$0$0
// test.kt:3 foo
// test.kt:7 box$lambda$0
// test.kt:3 foo
// test.kt:7 box

// test.kt:9 box
// test.kt:3 foo
// test.kt:10 box$lambda$1
// test.kt:3 foo
// test.kt:11 box$lambda$1$0
// test.kt:3 foo
// test.kt:12 box$lambda$1
// test.kt:3 foo
// test.kt:9 box

// test.kt:15 box
// test.kt:3 foo
// test.kt:17 box$lambda$2
// test.kt:3 foo
// test.kt:19 box$lambda$2$0
// test.kt:3 foo
// test.kt:20 box$lambda$2
// test.kt:3 foo
// test.kt:15 box

// test.kt:23 box
// test.kt:3 foo
// test.kt:26 box$lambda$3
// test.kt:3 foo
// test.kt:29 box$lambda$3$0
// test.kt:3 foo
// test.kt:30 box$lambda$3
// test.kt:3 foo
// test.kt:23 box

// test.kt:33 box

// EXPECTATIONS ClassicFrontend JVM_IR
// test.kt:7 box
// test.kt:3 foo
// test.kt:7 invoke
// test.kt:3 foo
// test.kt:7 invoke
// test.kt:3 foo
// test.kt:7 invoke
// test.kt:3 foo
// test.kt:7 box

// test.kt:9 box
// test.kt:3 foo
// test.kt:10 invoke
// test.kt:3 foo
// test.kt:11 invoke
// test.kt:3 foo
// test.kt:12 invoke
// test.kt:3 foo
// test.kt:9 box

// test.kt:15 box
// test.kt:3 foo
// test.kt:17 invoke
// test.kt:3 foo
// test.kt:19 invoke
// test.kt:3 foo
// test.kt:20 invoke
// test.kt:3 foo
// test.kt:15 box

// test.kt:23 box
// test.kt:3 foo
// test.kt:26 invoke
// test.kt:3 foo
// test.kt:29 invoke
// test.kt:3 foo
// test.kt:30 invoke
// test.kt:3 foo
// test.kt:23 box

// test.kt:33 box

// EXPECTATIONS WASM
// test.kt:7 $box (4)
// test.kt:3 $foo (37, 43, 37)
// test.kt:7 $box$lambda.invoke (10)
// test.kt:3 $foo (37, 43, 37)
// test.kt:7 $box$lambda$lambda.invoke (16, 18)
// test.kt:3 $foo (37, 46)
// test.kt:7 $box$lambda.invoke (20)
// test.kt:3 $foo (37, 46)
// test.kt:7 $box (4)

// test.kt:9 $box (4)
// test.kt:3 $foo (37, 43, 37)
// test.kt:10 $box$lambda.invoke (8)
// test.kt:3 $foo (37, 43, 37)
// test.kt:11 $box$lambda$lambda.invoke (12, 14)
// test.kt:3 $foo (37, 46)
// test.kt:12 $box$lambda.invoke (9)
// test.kt:3 $foo (37, 46)
// test.kt:9 $box (4)

// test.kt:15 $box (4)
// test.kt:3 $foo (37, 43, 37)
// test.kt:17 $box$lambda.invoke (8)
// test.kt:3 $foo (37, 43, 37)
// test.kt:19 $box$lambda$lambda.invoke (12, 19)
// test.kt:3 $foo (37, 46)
// test.kt:20 $box$lambda.invoke (9)
// test.kt:3 $foo (37, 46)
// test.kt:15 $box (4)

// test.kt:23 $box (4)
// test.kt:3 $foo (37, 43, 37)
// test.kt:26 $box$lambda.invoke (8)
// test.kt:3 $foo (37, 43, 37)
// test.kt:29 $box$lambda$lambda.invoke (12, 14)
// test.kt:3 $foo (37, 46)
// test.kt:30 $box$lambda.invoke (9)
// test.kt:3 $foo (37, 46)
// test.kt:23 $box (4)

// test.kt:33 $box (11, 4)

// EXPECTATIONS JS_IR
// test.kt:7 box
// test.kt:3 foo
// test.kt:7 box$lambda
// test.kt:3 foo
// test.kt:7 box$lambda$lambda

// test.kt:9 box
// test.kt:3 foo
// test.kt:12 box$lambda
// test.kt:3 foo
// test.kt:11 box$lambda$lambda

// test.kt:15 box
// test.kt:3 foo
// test.kt:20 box$lambda
// test.kt:3 foo
// test.kt:19 box$lambda$lambda

// test.kt:23 box
// test.kt:3 foo
// test.kt:30 box$lambda
// test.kt:3 foo
// test.kt:29 box$lambda$lambda
// test.kt:33 box

// EXPECTATIONS NATIVE
// test.kt:7 box
// test.kt:3 foo
// test.kt:7 invoke
// test.kt:3 foo
// test.kt:7 invoke
// test.kt:3 foo
// test.kt:7 invoke
// test.kt:3 foo
// test.kt:7 box

// test.kt:9 box
// test.kt:3 foo
// test.kt:9 invoke
// test.kt:10 invoke
// test.kt:3 foo
// test.kt:10 invoke
// test.kt:11 invoke
// test.kt:12 invoke
// test.kt:3 foo
// test.kt:10 invoke
// test.kt:13 invoke
// test.kt:3 foo
// test.kt:9 box

// test.kt:15 box
// test.kt:3 foo
// test.kt:15 invoke
// test.kt:17 invoke
// test.kt:3 foo
// test.kt:17 invoke
// test.kt:19 invoke
// test.kt:20 invoke
// test.kt:3 foo
// test.kt:17 invoke
// test.kt:21 invoke
// test.kt:3 foo
// test.kt:15 box

// test.kt:23 box
// test.kt:3 foo
// test.kt:24 invoke
// test.kt:26 invoke
// test.kt:3 foo
// test.kt:27 invoke
// test.kt:29 invoke
// test.kt:30 invoke
// test.kt:3 foo
// test.kt:26 invoke
// test.kt:31 invoke
// test.kt:3 foo
// test.kt:23 box

// test.kt:33 box
// test.kt:34 box
