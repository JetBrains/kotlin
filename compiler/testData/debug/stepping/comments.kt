// IGNORE_BACKEND: WASM
// FILE: test.kt

// Single line comment
fun box() {
    A().foo()
    A().bar()
}

/*
 Multi
 line

 comment
 */
class A {
    /**
     * Doc
     * comment
     */


    fun foo() {

    }

    // Single line comment 1

    // Single line comment 2
    fun bar() {

    }
}

// EXPECTATIONS JVM JVM_IR
// test.kt:6 box
// test.kt:16 <init>
// test.kt:6 box
// test.kt:25 foo
// test.kt:7 box
// test.kt:16 <init>
// test.kt:7 box
// test.kt:32 bar
// test.kt:8 box

// EXPECTATIONS JS_IR
// test.kt:6 box
// test.kt:16 <init>
// test.kt:6 box
// test.kt:25 foo
// test.kt:7 box
// test.kt:16 <init>
// test.kt:7 box
// test.kt:32 bar
// test.kt:8 box
