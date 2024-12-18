
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

// EXPECTATIONS JVM_IR
// test.kt:5 box
// test.kt:21 <clinit>
// test.kt:16 <init>
// test.kt:21 <clinit>
// test.kt:21 <clinit>
// test.kt:5 box
// test.kt:8 <init>
// test.kt:5 box
// test.kt:11 test
// test.kt:18 foo
// test.kt:19 foo
// test.kt:12 test
// test.kt:23 getProp
// test.kt:12 test
// test.kt:13 test
// test.kt:26 setProp
// test.kt:27 setProp
// test.kt:14 test
// test.kt:6 box

// EXPECTATIONS JS_IR
// test.kt:5 box
// test.kt:8 <init>
// test.kt:21 <init>
// test.kt:16 <init>
// test.kt:8 <init>
// test.kt:5 box
// test.kt:11 test
// test.kt:11 test
// test.kt:18 foo
// test.kt:19 foo
// test.kt:12 test
// test.kt:12 test
// test.kt:23 <get-prop>
// test.kt:13 test
// test.kt:14 test
// test.kt:6 box

// EXPECTATIONS WASM
// test.kt:5 $box (4)
// test.kt:8 $A.<init> (0)
// test.kt:21 $Companion.<init> (32)
// test.kt:28 $Companion.<init> (5)
// test.kt:8 $A.<init> (0)
// test.kt:29 $A.<init> (1)
// test.kt:5 $box (8)
// test.kt:11 $A.test (8)
// test.kt:18 $Companion.foo (20, 12)
// test.kt:19 $Companion.foo (9)
// test.kt:11 $A.test (8)
// test.kt:12 $A.test (8)
// test.kt:23 $Companion.<get-prop> (23, 16)
// test.kt:12 $A.test (8)
// test.kt:13 $A.test (15, 8)
// test.kt:26 $Companion.<set-prop> (16, 24, 16)
// test.kt:27 $Companion.<set-prop> (13)
// test.kt:14 $A.test (5)
// test.kt:5 $box (8)
// test.kt:6 $box (1)
