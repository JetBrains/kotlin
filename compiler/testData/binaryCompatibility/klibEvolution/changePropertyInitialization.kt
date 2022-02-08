// MODULE: lib
// FILE: A.kt
// VERSION: 1

val bar = 17
var muc = "first"
var toc = "second"
    get() = field


class X() {
    val bar = "third"
    var muc = 19
    var toc = "fourth"
       get() = field
}

// FILE: B.kt
// VERSION: 2

val bar = 23
var muc = "fifth"
var toc = "sixth"
    get() = field


class X() {
    val bar = "seventh"
    var muc = 29
    var toc = "eighth"
       get() = field
}


// MODULE: mainLib(lib)
// FILE: mainLib.kt
fun lib(): String {
    val x = X()

    return when {
        bar != 23 -> "fail 1"
        muc != "fifth" -> "fail 2"
        toc != "sixth" -> "fail 3"

        x.bar != "seventh" -> "fail 4"
        x.muc != 29 -> "fail 5"
        x.toc != "eighth" -> "fail 6"

        else -> "OK"
    }
}

// MODULE: main(mainLib)
// FILE: main.kt
fun box(): String = lib()

