class Prop {
    private val someProp = object { }
}



class Fun {
    private fun someFun() = object { }
}


class ArrayOfAnonymous {
    val a1 = arrayOf(
        object { val fy = "text"}
    )
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


abstract class Super {
    abstract val a: Any?
}


class Sub : Super() {
    override val a = arrayOf(
        object { val fy = "text"}
    )

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
