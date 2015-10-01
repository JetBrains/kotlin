open class A {
    companion object {
        class B
    }
}

class C: A() {
    val b: B = null!!

    init {
        B()
    }

    object O {
        val b: B = null!!

        init {
            B()
        }
    }

    class K {
        val b: B = null!!

        init {
            B()
        }
    }

    inner class I {
        val b: B = null!!

        init {
            B()
        }
    }
}