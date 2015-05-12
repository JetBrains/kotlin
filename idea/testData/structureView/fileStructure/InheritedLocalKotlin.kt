interface TA {
    fun some()
    val a: Int
}

class A : TA {
    override fun some() {
    }

    override val a: Int = 0
}

abstract class B : TA

// WITH_INHERITED