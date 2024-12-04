// MODULE: lib
// FILE: A.kt
// VERSION: 1

open class X {
    private fun foo1() = "private in super"
    fun testX1() = foo1()
    private fun foo2() = "private in super"
    fun testX2() = foo2()

    private val val1 = "private in super"
    fun testX3() = val1
    private val val2 = "private in super"
    fun testX4() = val2
}

// FILE: B.kt
// VERSION: 2

open class X {
    public fun foo1() = "public in super"
    fun testX1() = foo1()
    open public fun foo2() = "public in super"
    fun testX2() = foo2()

    public val val1 = "public in super"
    fun testX3() = val1
    open public val val2 = "public in super"
    fun testX4() = val2
}

// MODULE: mainLib(lib)
// FILE: mainLib.kt

class Y: X() {
    private fun foo1() = "private in derived"
    fun testY1() = foo1()
    private fun foo2() = "private in derived"
    fun testY2() = foo2()

    private val val1 = "private in derived"
    fun testY3() = val1
    private val val2 = "private in derived"
    fun testY4() = val2

}

fun lib(): String = when {
    X().testX1() != "public in super" -> "fail X().testX1()"
    Y().testX1() != "public in super" -> "fail Y().testX1()"
    Y().testY1() != "private in derived" -> "fail Y().testY1()"

    X().testX2() != "public in super" -> "fail X().testX2()"
    Y().testX2() != "public in super" -> "fail Y().testX2()"
    Y().testY2() != "private in derived" -> "fail Y().testY2()"

    X().testX3() != "public in super" -> "fail X().testX3()"
    Y().testX3() != "public in super" -> "fail Y().testX3()"
    Y().testY3() != "private in derived" -> "fail Y().testY3()"

    X().testX4() != "public in super" -> "fail X().testX4()"
    Y().testX4() != "public in super" -> "fail Y().testX4()"
    Y().testY4() != "private in derived" -> "fail Y().testY4()"

    else -> "OK"
}

// MODULE: main(mainLib)
// FILE: main.kt
fun box(): String = lib()

