class Prop {
    private val someProp = object { }
}

private class C(val y: Int) {
    val initChild = { ->
        object {
            override fun toString(): String {
                return "child" + y
            }
        }
    }
}


class ValidPublicSupertype {
    val x = object : Runnable {
        override fun run() {}
    }

    fun bar() = object : Runnable {
        override fun run() {}
    }
}

interface I
class InvalidPublicSupertype {
    val x = object : Runnable, I  {
        override fun run() {}
    }

    fun bar() = object : Runnable, I {
        override fun run() {}
    }
}
// COMPILATION_ERRORS