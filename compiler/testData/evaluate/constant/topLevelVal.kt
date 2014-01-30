package test

val a = 1

// val prop1: 1.toInt()
val prop1 = a

// val prop2: 2.toInt()
val prop2 = a + 1

class A {
    // val prop3: 1.toInt()
    val prop3 = a

    // val prop4: 2.toInt()
    val prop4 = a + 1

    val b = {
        // val prop11: 1.toInt()
        val prop11 = a

        // val prop12: 2.toInt()
        val prop12 = a + 1
    }

    val c = object: Foo {
        override fun f() {
            // val prop9: 1.toInt()
            val prop9 = a

            // val prop10: 2.toInt()
            val prop10 = a + 1
        }
    }
}

fun foo() {
    // val prop5: 1.toInt()
    val prop5 = a

    // val prop6: 2.toInt()
    val prop6 = a + 1
}

trait Foo {
    fun f()
}