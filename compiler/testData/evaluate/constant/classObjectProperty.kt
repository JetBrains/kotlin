package test

// val prop1: 1.toInt()
val prop1 = A.a

// val prop2: 2.toInt()
val prop2 = A.a + 1

class A {
    // val prop3: 1.toInt()
    val prop3 = A.a

    // val prop4: 2.toInt()
    val prop4 = A.a + 1

    class object {
        val a = 1
    }
}

fun foo() {
    // val prop5: 1.toInt()
    val prop5 = A.a

    // val prop6: 2.toInt()
    val prop6 = A.a + 1

    val b = {
        // val prop11: 1.toInt()
        val prop11 = A.a

        // val prop12: 2.toInt()
        val prop12 = A.a + 1
    }

    val c = object: Foo {
        override fun f() {
            // val prop9: 1.toInt()
            val prop9 = A.a

            // val prop10: 2.toInt()
            val prop10 = A.a + 1
        }
    }
}

trait Foo {
    fun f()
}