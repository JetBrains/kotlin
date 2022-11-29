// MODULE: lib
// FILE: A.kt
// VERSION: 1

val bar = "original global value"
var muc = "original global value"
var toc = "original global value"

class X() {
    val qux = "original member value" 
    var nis = "original member value" 
    var roo = "original member value" 
}

// FILE: B.kt
// VERSION: 2

val bar 
    get() = "changed global value of val"
var muc = "initialized global value of var with field"
    get() = field
    set(value) {
        field = "changed global value of var with field"
    }
var toc 
    get() = "changed global value of var without field"
    set(value) { }


class X() {
    val qux 
        get() = "changed member value of val"
    var nis = "initialized member value of var with field"
        get() = field
        set(value) {
            field = "changed member value of var with field"
        }
    var roo = "initialized member value of var without field"
        get() = "changed member value of var without field"
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
        bar != "changed global value of val" -> "fail 1"
        muc != "changed global value of var with field" -> "fail 2"
        toc != "changed global value of var without field" -> "fail 3"

        x.qux != "changed member value of val" -> "fail 4"
        x.nis != "changed member value of var with field" -> "fail 5"
        x.roo != "changed member value of var without field" -> "fail 5"

        else -> "OK"
    }
}

// MODULE: main(mainLib)
// FILE: main.kt
fun box(): String = lib()

