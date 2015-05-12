package test

val a = 1

// val prop1: 1
val prop1 = a

// val prop2: 2
val prop2 = a + 1

class A {
    // val prop3: 1
    val prop3 = a

    // val prop4: 2
    val prop4 = a + 1

    val b = {
        // val prop11: 1
        val prop11 = a

        // val prop12: 2
        val prop12 = a + 1
    }

    val c = object: Foo {
        override fun f() {
            // val prop9: 1
            val prop9 = a

            // val prop10: 2
            val prop10 = a + 1
        }
    }
}

fun foo() {
    // val prop5: 1
    val prop5 = a

    // val prop6: 2
    val prop6 = a + 1
}

interface Foo {
    fun f()
}