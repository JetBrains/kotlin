// ISSUE: KT-66148
// FILE: test.kt
class Controller<E>

fun <E1> generate(block: Controller<E1>.() -> Unit) {
    block(Controller())
}

class A(val r: String)

fun foo(c: Controller<String>): A = A("OK")

fun bar(
    propertyForInvoke: A.() -> Unit,
) {
    generate {
        foo(this).propertyForInvoke()
    }
}

fun box(): String {
    var result = "fail"
    bar {
        result = r
    }

    return result
}

// EXPECTATIONS JVM_IR
// test.kt:22 box
// test.kt:23 box
// test.kt:16 bar
// test.kt:6 generate
// test.kt:3 <init>
// test.kt:6 generate
// EXPECTATIONS ClassicFrontend JVM_IR
// test.kt:17 invoke
// EXPECTATIONS FIR JVM_IR
// test.kt:17 bar$lambda$0
// EXPECTATIONS JVM_IR
// test.kt:11 foo
// test.kt:9 <init>
// test.kt:11 foo
// EXPECTATIONS ClassicFrontend JVM_IR
// test.kt:17 invoke
// test.kt:24 invoke
// EXPECTATIONS FIR JVM_IR
// test.kt:17 bar$lambda$0
// test.kt:24 box$lambda$1
// EXPECTATIONS JVM_IR
// test.kt:9 getR
// EXPECTATIONS ClassicFrontend JVM_IR
// test.kt:24 invoke
// test.kt:25 invoke
// test.kt:17 invoke
// test.kt:18 invoke
// EXPECTATIONS FIR JVM_IR
// test.kt:24 box$lambda$1
// test.kt:25 box$lambda$1
// test.kt:17 bar$lambda$0
// test.kt:18 bar$lambda$0
// EXPECTATIONS JVM_IR
// test.kt:6 generate
// test.kt:7 generate
// test.kt:19 bar
// test.kt:27 box

// EXPECTATIONS JS_IR
// test.kt:22 box
// test.kt:23 box
// test.kt:23 box$lambda
// test.kt:23 box
// test.kt:16 bar
// test.kt:16 bar$lambda
// test.kt:16 bar
// test.kt:6 generate
// test.kt:3 <init>
// test.kt:6 generate
// test.kt:17 bar$lambda$lambda
// test.kt:11 foo
// test.kt:9 <init>
// test.kt:9 <init>
// test.kt:17 bar$lambda$lambda
// test.kt:24 box$lambda$lambda
// test.kt:25 box$lambda$lambda
// test.kt:18 bar$lambda$lambda
// test.kt:7 generate
// test.kt:19 bar
// test.kt:27 box

// EXPECTATIONS WASM
// test.kt:22 $box (17, 4)
// test.kt:23 $box (8, 8, 4)
// test.kt:16 $bar (13, 13, 13, 4)
// test.kt:6 $generate (4, 10, 10, 4, 4)
// test.kt:3 $Controller.<init>
// test.kt:17 $bar$lambda.invoke (18, 12, 8, 18, 18, 37)
// test.kt:11 $foo (36, 38, 38, 38, 38, 36, 43)
// test.kt:9 $A.<init> (8, 8, 8, 22)
// test.kt:24 $box$lambda.invoke (8, 17, 17, 8, 18)
// test.kt:7 $generate
// test.kt:19 $bar
// test.kt:27 $box (11, 11, 11, 4)
