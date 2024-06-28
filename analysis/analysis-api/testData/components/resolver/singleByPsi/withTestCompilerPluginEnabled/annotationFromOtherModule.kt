// WITH_FIR_TEST_COMPILER_PLUGIN
// MODULE: lib

// FILE: foo/MyAnnotation.kt
package foo

annotation class MyAnnotation

// MODULE: main(lib)

// FILE: test/main.kt
package test

@foo.MyAnnotation
class MyClass

fun test(myClass: MyClass) {
    myClass.mater<caret>ialize()
}