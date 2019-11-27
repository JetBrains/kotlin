class Foo {
    var a: String

    init {
        a = x()
    }
}

class Bar {
    init {
        val a = 5
    }

    init {
        val b = 2
    }
}

class Boo {
    init {
        val a = 5
    }

    val x = x()

    init {
        val b = 2
    }
}

class Zoo {
    init { val a = 5 }

    init { val b = 6 }

    init {
        val c = 7
    }

    init { val d = 8 }
}

fun x() = ""

// IGNORE_BACKEND: JVM_IR
// 2 2 1 4 5 6 9 10 11 12 14 15 16 24 19 20 21 22 24 26 27 28 31 32 34 36 37 38 40 43