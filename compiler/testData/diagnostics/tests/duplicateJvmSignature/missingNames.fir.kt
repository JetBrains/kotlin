// !DIAGNOSTICS: -DUPLICATE_CLASS_NAMES
fun () {

}

fun Outer.() {

}

val<!SYNTAX!><!> : Int = 1

class<!SYNTAX!><!> {

}

object<!SYNTAX!><!> {

}

interface<!SYNTAX!><!> {

}

enum class<!SYNTAX!><!> {

}

annotation class<!SYNTAX!><!> {

}

class Outer {
    fun () {

    }

    val<!SYNTAX!><!> : Int = 1

    class<!SYNTAX!><!> {

    }

    object<!SYNTAX!><!> {

    }

    interface<!SYNTAX!><!> {

    }

    enum class<!SYNTAX!><!> {

    }

    annotation class<!SYNTAX!><!> {

    }
}

fun outerFun() {
    fun () {

    }
    fun () {

    }
}

