package test

class A() {
    fun foo() {
        val a = 1

        // val prop5: 1
        val prop5 = a

        // val prop6: 2
        val prop6 = a + 1

        fun local() {
            // val prop1: 1
            val prop1 = a

            // val prop2: 2
            val prop2 = a + 1
        }

        val b = {
            // val prop3: 1
            val prop3 = a

            // val prop4: 2
            val prop4 = a + 1
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
}

fun foo() {
    val a = 1

    // val prop7: 1
    val prop7 = a

    // val prop8: 2
    val prop8 = a + 1
}

interface Foo {
    fun f()
}