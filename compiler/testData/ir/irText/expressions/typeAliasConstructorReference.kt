import Host.Nested

class C(x: Int)

typealias CA = C

object Host {
    class Nested(x: Int)
}

typealias NA = Nested

val test1: (Int) -> CA = ::CA
val test2: (Int) -> NA = ::NA
