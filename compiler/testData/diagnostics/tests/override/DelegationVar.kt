package delegation

trait Aaa {
    var i: Int
}

class Bbb(aaa: Aaa) : Aaa by aaa
