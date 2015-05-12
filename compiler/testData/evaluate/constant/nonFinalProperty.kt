package test

// val prop1: null
val prop1 = A().a

// val prop2: null
val prop2 = A().a + 1

class A() {
    var a = 1

    // val prop3: null
    val prop3 = a

    // val prop4: null
    val prop4 = a + 1

    fun foo() {
        // val prop5: null
        val prop5 = A().a

        // val prop6: null
        val prop6 = A().a + 1

        val b = {
            // val prop11: null
            val prop11 = A().a

            // val prop12: null
            val prop12 = A().a + 1
        }

        val c = object: Foo {
            override fun f() {
                // val prop9: null
                val prop9 = A().a

                // val prop10: null
                val prop10 = A().a + 1
            }
        }
    }

}

fun foo() {
    // val prop7: null
    val prop7 = A().a

    // val prop8: null
    val prop8 = A().a + 1
}

interface Foo {
    fun f()
}