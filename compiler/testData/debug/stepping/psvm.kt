// FILE: test.kt

fun main(args: Array<String>) {
    args[0]
}

fun box() {
    main(arrayOf("OK"))
}

// EXPECTATIONS JVM JVM_IR
// test.kt:8 box
// test.kt:4 main
// test.kt:5 main
// test.kt:9 box

// EXPECTATIONS JS_IR
// test.kt:8 box
// test.kt:4 main
// test.kt:5 main
// test.kt:9 box