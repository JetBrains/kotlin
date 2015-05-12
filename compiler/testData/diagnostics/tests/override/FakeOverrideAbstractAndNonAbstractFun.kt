open class Ccc() {
    fun foo() = 1
}

interface Ttt {
    fun foo(): Int
}

class Zzz() : Ccc(), Ttt
