// MODULE: lib
// FILE: A.kt
// VERSION: 1

val bar 
    get() = "original global value of val"
var muc = "initialized global value of var with field"
    get() = field
    set(value) {
        field = "original global value of var with field"
    }
var toc 
    get() = "original global value of var without field"
    set(value) { }


class X() {
    val qux 
        get() = "original member value of val"
    var nis = "initialized member value of var with field"
        get() = field
        set(value) {
            field = "original member value of var with field"
        }
    var roo = "initialized member value of var without field"
        get() = "original member value of var without field"
}

// FILE: B.kt
// VERSION: 2

val bar = "changed global value"
var muc = "changed global value"
var toc = "changed global value"

class X() {
    val qux = "changed member value" 
    var nis = "changed member value" 
    var roo = "changed member value" 
}

// MODULE: mainLib(lib)
// FILE: mainLib.kt
fun lib(): String {
    val x = X()
    muc = "first"
    toc = "second"
    x.nis = "third"
    x.roo = "fourth"

    return when {
        bar != "changed global value" -> "fail 1"
        muc != "first" -> "fail 2"
        toc != "second" -> "fail 3"

        x.qux != "changed member value" -> "fail 4"
        x.nis != "third" -> "fail 5"
        x.roo != "fourth" -> "fail 6"

        else -> "OK"
    }
}

// MODULE: main(mainLib)
// FILE: main.kt
fun box(): String = lib()

