fun foo1(vararg params: Int) {
    println(params)
}

typealias MyInt = Int
fun foo2(vararg params: MyInt) {
    println(params)
}