class Test1 {
    init {
        println()
    }
}

class Test2(val x: Int) {
    init {
        println()
    }
}

class Test3 {
    init {
        println()
    }

    constructor()
}

class Test4 {
    init {
        println("1")
    }

    constructor()

    init {
        println("2")
    }
}

class Test5 {
    init {
        println("1")
    }

    inner class TestInner {
        init {
            println("2")
        }
    }
}