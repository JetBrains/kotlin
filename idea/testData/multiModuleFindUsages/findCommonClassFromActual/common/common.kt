fun test() {
    My("a").boo()
    My("b").boo()
}

expect class My(val s: String) {
    fun boo()
}