// IGNORE_BACKEND: WASM
// FILE: test.kt

enum class E() {
    A,
    B;

    fun foo() = {
        prop
    }

    val prop = 22
}

enum class E2(val y : Int) {
    C(1),
    D(
        2
    )
}

fun box() {
    E.A.foo()
    E2.C;
}

// JVM_IR maintains line number information in the class initializer for the
// initialization of the enum entries. There is line number information for
// the allocation of the object, for the evaluation of arguments to the
// constructor, and for the call to the constructor. This is consistent
// with the line number information generated for normal object allocation.

// JVM has no line number information in <clinit> if there are no arguments
// to the enum constructor. If there are arguments it has line number information
// for the evaluation of the arguments constructor and for the constructor call,
// but not for the allocation of the object.

// EXPECTATIONS JVM JVM_IR
// test.kt:23 box
// EXPECTATIONS JVM_IR
// test.kt:5 <clinit>
// test.kt:6 <clinit>
// EXPECTATIONS JVM JVM_IR
// test.kt:8 foo
// test.kt:10 foo
// test.kt:23 box
// test.kt:24 box
// test.kt:16 <clinit>
// EXPECTATIONS JVM_IR
// test.kt:17 <clinit>
// EXPECTATIONS JVM JVM_IR
// test.kt:18 <clinit>
// test.kt:17 <clinit>
// test.kt:25 box

// EXPECTATIONS JS_IR
// test.kt:23 box
// test.kt:12 <init>
// test.kt:4 <init>
// test.kt:12 <init>
// test.kt:4 <init>
// test.kt:23 box
// test.kt:10 foo
// test.kt:8 E$foo$lambda
// test.kt:16 E2_initEntries
// test.kt:15 <init>
// test.kt:15 <init>
// test.kt:18 E2_initEntries
// test.kt:15 <init>
// test.kt:15 <init>
// test.kt:25 box
