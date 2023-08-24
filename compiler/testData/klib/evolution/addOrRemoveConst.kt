// MODULE: lib
// FILE: A.kt
// VERSION: 1

val bar: String
    get() = "a val with a getter"

const val qux: String = "a const val"

object X {
    val zem: String
        get() = "a member val with a getter"

    const val spi: String = "a member const val"
}

// FILE: B.kt
// VERSION: 2

const val bar: String = "a val turned into a const"

val qux: String 
    get() = "a const turned into a val with a getter"

object X {
    val zem: String
        get() = "a member val turned into a const"

    const val spi: String = "a member const turned into a val with a getter"
}

// MODULE: mainLib(lib)
// FILE: mainLib.kt

fun lib(): String {
    return when {
        bar != "a val turned into a const" -> "fail 1"
        qux != "a const turned into a val with a getter" -> "fail 2"
        X.zem != "a member val turned into a const" -> "fail 1"
        X.spi != "a member const turned into a val with a getter" -> "fail 2"

        else -> "OK"
    }
}

// MODULE: main(mainLib)
// FILE: main.kt
fun box(): String = lib()

