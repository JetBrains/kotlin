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

// LINENUMBERS
// test.kt:22 box
// LINENUMBERS JVM_IR
// test.kt:4 <clinit>
// test.kt:5 <clinit>
// LINENUMBERS
// test.kt:7 foo
// test.kt:9 foo
// test.kt:22 box
// test.kt:23 box
// test.kt:15 <clinit>
// LINENUMBERS JVM_IR
// test.kt:16 <clinit>
// LINENUMBERS
// test.kt:17 <clinit>
// test.kt:16 <clinit>
// test.kt:24 box