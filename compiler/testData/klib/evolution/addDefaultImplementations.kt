// MODULE: lib
// FILE: A.kt
// VERSION: 1

interface X {
    fun foo(): String
    val bar: String
    fun qux(): String
}

interface Z {
    fun qux(): String = "initially default method"
}

// FILE: B.kt
// VERSION: 2

interface X {
    fun foo(): String = "default method"
    val bar: String get() = "default property"
    fun qux(): String = "ubdated default method"
}

interface Z {
    fun qux(): String = "alternative default method"
}

// MODULE: mainLib(lib)
// FILE: mainLib.kt

class Y: X, Z {
    override fun foo(): String = "overridden method"
    override val bar: String get() = "overridden property"
    override fun qux(): String = "overridden multiple versions"
}

val y = Y()
val t = object : Z {}

fun lib(): String = when {
    y.foo() != "overridden method" -> "fail 1"
    y.bar != "overridden property" -> "fail 2"
    y.qux() != "overridden multiple versions" -> "fail 3"
    t.qux() != "alternative default method" -> "fail 4"

    else -> "OK"
}

// MODULE: main(mainLib)
// FILE: main.kt
fun box(): String = lib()

