class Klass {
    private val localObject = object {
        fun f() {
        }

        val p = 5
    }

    init {
        localObject.f()
        localObject.p
    }


    private fun localObject2() = object {
        fun f() {
        }

        val p = 5
    }

    init {
        localObject2().f()
        localObject2().p
    }


    private val a = object {
        val b = object {
            val c = object {
                val d = 5
            }
        }
    }

    init {
        a.b.c.d
    }
}

fun main(args: Array<String>) {
    Klass()
}