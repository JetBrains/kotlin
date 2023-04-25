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
// test.kt:5 box
// test.kt:15 <init>
// test.kt:5 box
// test.kt:24 foo
// test.kt:6 box
// test.kt:15 <init>
// test.kt:6 box
// test.kt:31 bar
// test.kt:7 box

// EXPECTATIONS JS_IR
// test.kt:5 box
// test.kt:15 <init>
// test.kt:5 box
// test.kt:24 foo
// test.kt:6 box
// test.kt:15 <init>
// test.kt:6 box
// test.kt:31 bar
// test.kt:7 box
