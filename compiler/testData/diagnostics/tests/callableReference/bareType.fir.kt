fun f1() = Map::hashCode
fun f2() = Map.Entry::hashCode

class Outer<T> {
    inner class Inner
}

fun f3() = Outer.Inner::hashCode
