trait TA {
    fun some()
}

class A : TA {
    override fun some() {
    }
}

abstract class B : TA

// WITH_INHERITED