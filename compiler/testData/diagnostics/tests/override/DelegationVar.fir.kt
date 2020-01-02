package delegation

interface Aaa {
    var i: Int
}

class Bbb(aaa: Aaa) : Aaa by aaa
