class A {
    fun component1() = 1
    fun component2() = 2
}


fun box() : String {
    val (a, b) = A()

    val local = object {
        public fun run() : Int {
            return a
        }
    }
    return if (local.run() == 1 && b == 2) "OK" else "fail"
}
