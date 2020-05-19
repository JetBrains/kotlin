// FILE: test.kt

class A {
    companion object {
        val prop0 = 1
        val prop1 = 2
        fun foo(): Int {
            return prop0 + prop1
        }
    }
}

fun box() {
    A.prop0
    A.prop1
    A.foo()
}

// The JVM version hits get getProp line numbers twice. That appears
// to be because the synthetic accessibility bridges (access$getProp0$cp)
// have line numbers (of the start of the surrounding class) in the JVM
// version and they do not have line numbers in the JVM_IR version.

// LINENUMBERS
// test.kt:14 box
// test.kt:5 getProp0
// LINENUMBERS JVM
// test.kt:5 getProp0
// LINENUMBERS
// test.kt:14 box
// test.kt:15 box
// test.kt:6 getProp1
// LINENUMBERS JVM
// test.kt:6 getProp1
// LINENUMBERS
// test.kt:15 box
// test.kt:16 box
// test.kt:8 foo
// test.kt:5 getProp0
// LINENUMBERS JVM
// test.kt:5 getProp0
// LINENUMBERS
// test.kt:8 foo
// test.kt:6 getProp1
// LINENUMBERS JVM
// test.kt:6 getProp1
// LINENUMBERS
// test.kt:8 foo
// test.kt:16 box
// test.kt:17 box
