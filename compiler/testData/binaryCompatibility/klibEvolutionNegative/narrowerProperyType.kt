// MODULE: lib
// FILE: A.kt
// VERSION: 1

val bar: Any = "global Any initialization value"
val qux: String? = "global String? initialization value"
val wuw: Any = "another global Any initialization value"

class X {
    val miz: Any = "member Any initialization value"
    val zur: String? = "member String? initialization value"
    val kog: Any = "another member Any initialization value"
}

// FILE: B.kt
// VERSION: 2

val bar: String = "first"
val qux: String = "second"
val wuw: Int = 17

class X {
    val miz: String = "third"
    val zur: String = "fourth"
    val kog: Int = 19
}

// MODULE: mainLib(lib)
// FILE: mainLib.kt
fun lib(): String {
    val x = X()

    return when {
        bar != "first" -> "fail 1"
        qux != "second" -> "fail 2"
        wuw != 17 -> "fail 3"

        x.miz != "third" -> "fail 1"
        x.zur != "fourth" -> "fail 2"
        x.kog != 19 -> "fail 3"

        else -> "OK"
    }
}

// MODULE: main(mainLib)
// FILE: main.kt
fun box(): String = lib()

