package delegation

interface Aaa {
    val i: Int
}

class Bbb(aaa: Aaa) : Aaa by aaa
