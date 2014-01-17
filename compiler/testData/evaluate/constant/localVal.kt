package test

class A() {
    fun foo() {
        val a = 1

        // val prop5: 1.toInt()
        val prop5 = a

        // val prop6: 2.toInt()
        val prop6 = a + 1

        fun local() {
            // val prop1: 1.toInt()
            val prop1 = a

            // val prop2: 2.toInt()
            val prop2 = a + 1
        }

        val b = {
            // val prop3: 1.toInt()
            val prop3 = a

            // val prop4: 2.toInt()
            val prop4 = a + 1
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
}

fun foo() {
    val a = 1

    // val prop7: 1.toInt()
    val prop7 = a

    // val prop8: 2.toInt()
    val prop8 = a + 1
}

trait Foo {
    fun f()
}