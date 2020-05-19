// FILE: test.kt

interface A {
    fun foo() = 32

    fun bar(): Int {
        return foo()
    }
}

fun box() {
    (object : A {}).bar()
}

// The JVM backend generates non-synthetic overrides of foo and bar
// in the object both with line number 12. That means that there will
// be steps on line number 12 on entry and exit to both bar and foo.

// TODO: Is this what we want? Should they be marked as bridges instead?
// Doesn't look like the intellij debugger skips non-synthetic bridges?
// There seems to be some heuristics in intellij dealing with this as
// the stepping behavior with repeated step-into is mostly OK.

// IGNORE_BACKEND: JVM_IR
// The JVM_IR backend generates non-synthetic overrides of foo and bar
// with no line numbers. That leads to steps on line -1 but only on
// exit from bar and foo.

// LINENUMBERS
// test.kt:12 box
// test.kt:12 <init>
// test.kt:12 box
// test.kt:12 bar
// test.kt:7 bar
// test.kt:12 foo
// test.kt:4 foo
// test.kt:12 foo
// test.kt:7 bar
// test.kt:12 bar
// test.kt:12 box
// test.kt:13 box
