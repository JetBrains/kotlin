
fun box(): String {
    var a = 0
    fun <T> local(xx: T): T {
        class A {
            val b = 0
            fun id(x: T): T {
                a = b
                return x
            }

        }
        fun local2() : T {
            return A().id(xx)
        }
        return local2()
    }
    return local("OK")
}
