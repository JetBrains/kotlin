open class Ccc() {
    fun foo() = 1
}

trait Ttt {
    fun foo(): Int
}

class Zzz() : Ccc(), Ttt
