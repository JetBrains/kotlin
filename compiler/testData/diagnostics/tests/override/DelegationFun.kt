package delegation

interface Aaa {
    fun foo()
}

class Bbb(aaa: Aaa) : Aaa by aaa
