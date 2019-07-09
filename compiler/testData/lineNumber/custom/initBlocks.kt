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

fun x() = ""

// IGNORE_BACKEND: JVM_IR
// 2 2 1 5 6 9 11 12 15 16 24 19 21 22 24 27 28 31