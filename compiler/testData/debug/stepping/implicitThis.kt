// FILE: test.kt

fun box() {
    A().test()
}

class A {
    fun test() {
        //Breakpoint!
        foo()
        prop
        prop = 2
    }

    companion object {
        private fun foo() {
            val a = 1
        }

        private var prop: Int = 2
            get() {
                return 1
            }
            set(i: Int) {
                field = i
            }
    }
}

// EXPECTATIONS JVM JVM_IR
// test.kt:4 box
// test.kt:20 <clinit>
// test.kt:15 <init>
// test.kt:20 <clinit>
// test.kt:20 <clinit>
// test.kt:4 box

// test.kt:7 <init>
// test.kt:4 box
// test.kt:10 test
// test.kt:17 foo
// test.kt:18 foo
// test.kt:11 test
// test.kt:22 getProp
// test.kt:11 test
// test.kt:12 test
// test.kt:25 setProp
// test.kt:26 setProp
// test.kt:13 test
// test.kt:5 box

// EXPECTATIONS JS_IR
// test.kt:4 box
// test.kt:7 <init>
// test.kt:20 <init>
// test.kt:15 <init>
// test.kt:7 <init>
// test.kt:4 box
// test.kt:10 test
// test.kt:10 test
// test.kt:17 foo
// test.kt:18 foo
// test.kt:11 test
// test.kt:11 test
// test.kt:22 <get-prop>
// test.kt:12 test
// test.kt:13 test
// test.kt:5 box
