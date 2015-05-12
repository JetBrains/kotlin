fun main(args: Array<String>) {
    val localObject = object {
        fun f() {
        }

        val p = 5
    }

    localObject.f()
    localObject.p


    fun localObject2() = object {
        fun f() {
        }

        val p = 5
    }

    localObject2().f()
    localObject2().p


    val a = object {
        val b = object {
            val c = object {
                val d = 5
            }
        }
    }

    a.b.c.d


    class LocalClass {
        fun f() {
        }

        val p = 5
    }


    LocalClass().f()
    LocalClass().p
}