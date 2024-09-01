fun foo1(vararg params: Int) {
    params.hashCode()
}

typealias MyInt = Int
fun foo2(vararg params: MyInt) {
    params.hashCode()
}