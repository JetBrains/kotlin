// WITH_STDLIB

// FILE: test.kt
fun test(x: Int) {
    println(x)
    println("split")
    println(x)

    println(O.y)
    println("split")
    println(O.y)
}

object O {
    val y = 23
}

fun box() {
    test(0)
}

// EXPECTATIONS JVM_IR
// test.kt:19 box
// test.kt:5 test
// test.kt:6 test
// test.kt:7 test
// test.kt:9 test
// test.kt:15 <clinit>
// test.kt:15 getY
// test.kt:9 test
// test.kt:10 test
// test.kt:11 test
// test.kt:15 getY
// test.kt:11 test
// test.kt:12 test
// test.kt:20 box

// EXPECTATIONS JS_IR
// test.kt:19 box
// test.kt:5 test
// test.kt:6 test
// test.kt:7 test
// test.kt:9 test
// test.kt:15 <init>
// test.kt:14 <init>
// test.kt:9 test
// test.kt:10 test
// test.kt:11 test
// test.kt:11 test
// test.kt:12 test
// test.kt:20 box

// EXPECTATIONS WASM
// test.kt:19 $box (9, 4)
// test.kt:5 $test (12, 4)
// test.kt:6 $test (12, 4)
// test.kt:7 $test (12, 4)
// test.kt:15 $O.<init> (12)
// test.kt:16 $O.<init> (1)
// test.kt:9 $test (14, 4)
// test.kt:10 $test (12, 4)
// test.kt:11 $test (14, 4)
// test.kt:12 $test (1)
// test.kt:20 $box (1)

// EXPECTATIONS NATIVE
// test.kt:19 box
// test.kt:4 test
// test.kt:5 test
// test.kt:5 test
// test.kt:6 test
// test.kt:6 test
// test.kt:7 test
// test.kt:7 test
// test.kt:7 test
// test.kt:9 test
// test.kt:14 <get-$instance>
// test.kt:1 <get-$instance>
// test.kt:16 <get-$instance>
// test.kt:9 test
// test.kt:15 <get-y>
// test.kt:9 test
// test.kt:9 test
// test.kt:9 test
// test.kt:10 test
// test.kt:10 test
// test.kt:11 test
// test.kt:14 <get-$instance>
// test.kt:1 <get-$instance>
// test.kt:16 <get-$instance>
// test.kt:11 test
// test.kt:15 <get-y>
// test.kt:11 test
// test.kt:11 test
// test.kt:12 test
// test.kt:20 box
