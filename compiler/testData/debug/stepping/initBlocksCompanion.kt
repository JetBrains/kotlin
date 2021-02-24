// FILE: test.kt

class A {
    companion object {
        val s: String

        init {
            s = "OK"
        }

        val x = x()

        init { val a = 32 }

        init {
            val b = 42
        }

        init
        {
            val c = 43
        }
    }
}

fun x() = ""

fun box() {
    A.x
    A.s
}

// LINENUMBERS
// test.kt:29 box
// test.kt:7 <clinit>
// test.kt:8 <clinit>
// test.kt:9 <clinit>
// test.kt:11 <clinit>
// test.kt:13 <clinit>
// test.kt:15 <clinit>
// test.kt:16 <clinit>
// test.kt:17 <clinit>
// test.kt:19 <clinit>
// test.kt:21 <clinit>
// test.kt:22 <clinit>
// test.kt:11 getX
// test.kt:11 getX
// test.kt:29 box
// test.kt:30 box
// test.kt:5 getS
// test.kt:5 getS
// test.kt:30 box
// test.kt:31 box
