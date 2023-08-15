// MODULE: lib
// FILE: A.kt
// VERSION: 1

lateinit var qux: String 

class X {
    lateinit var bar: String
}

// FILE: B.kt
// VERSION: 2

var qux: String = "initialized global"

class X {
    var bar: String = "initialized member"
}

// MODULE: mainLib(lib)
// FILE: mainLib.kt

val x = X()

fun lib(): String {

    val a = qux
    val b = x.bar
    qux = "new global value"
    x.bar = "new member value"

    return when {
        a != "initialized global" -> "fail 1"
        b != "initialized member" -> "fail 2"
        qux != "new global value" -> "fail 3"
        x.bar != "new member value" -> "fail 4"

        else -> "OK"
    }
}

// MODULE: main(mainLib)
// FILE: main.kt
fun box(): String = lib()

