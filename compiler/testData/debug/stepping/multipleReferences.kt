// WITH_STDLIB
// IGNORE_NATIVE: useThreadStateChecker=ENABLED && cacheMode=STATIC_ONLY_DIST
// IGNORE_NATIVE: useThreadStateChecker=ENABLED && cacheMode=STATIC_EVERYWHERE
// IGNORE_NATIVE: useThreadStateChecker=ENABLED && cacheMode=STATIC_PER_FILE_EVERYWHERE

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

// EXPECTATIONS JVM
// test.kt:22 box
// test.kt:8 test
// test.kt:9 test
// test.kt:10 test
// test.kt:12 test
// test.kt:18 <clinit>
// test.kt:18 getY
// test.kt:12 test
// test.kt:13 test
// test.kt:14 test
// test.kt:18 getY
// test.kt:14 test
// test.kt:15 test
// test.kt:23 box

// EXPECTATIONS NATIVE
// test.kt:22 box
// test.kt:7 test
// test.kt:8 test
// test.kt:8 test
// test.kt:9 test
// test.kt:9 test
// test.kt:10 test
// test.kt:10 test
// test.kt:10 test
// test.kt:12 test
// test.kt:17 <get-$instance>
// test.kt:1 <get-$instance>
// test.kt:19 <get-$instance>
// test.kt:12 test
// test.kt:18 <get-y>
// test.kt:12 test
// test.kt:12 test
// test.kt:12 test
// test.kt:13 test
// test.kt:13 test
// test.kt:14 test
// test.kt:17 <get-$instance>
// test.kt:1 <get-$instance>
// test.kt:19 <get-$instance>
// test.kt:14 test
// test.kt:18 <get-y>
// test.kt:14 test
// test.kt:14 test
// test.kt:15 test
// test.kt:23 box

// EXPECTATIONS JS_IR
// test.kt:22 box
// test.kt:8 test
// test.kt:9 test
// test.kt:10 test
// test.kt:12 test
// test.kt:18 <init>
// test.kt:17 <init>
// test.kt:12 test
// test.kt:13 test
// test.kt:14 test
// test.kt:14 test
// test.kt:15 test
// test.kt:23 box

// EXPECTATIONS WASM
// test.kt:22 $box (9, 4)
// test.kt:8 $test (12, 4)
// test.kt:9 $test (12, 4)
// test.kt:10 $test (12, 4)
// test.kt:18 $O.<init> (12)
// test.kt:19 $O.<init> (1)
// test.kt:12 $test (14, 4)
// test.kt:13 $test (12, 4)
// test.kt:14 $test (14, 4)
// test.kt:15 $test (1)
// test.kt:23 $box (1)
