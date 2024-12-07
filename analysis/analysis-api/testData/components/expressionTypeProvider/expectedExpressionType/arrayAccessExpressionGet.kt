class A {
    operator fun get(p1: Int) {}
    operator fun set(p1: String, p2: Int, value: Int) {}

    fun d() = this[a<caret>v]

}
