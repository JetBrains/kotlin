package delegation

trait Aaa {
    fun foo()
}

class Bbb(aaa: Aaa) : Aaa by aaa
