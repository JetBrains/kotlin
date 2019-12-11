fun test() {
    fun <T> foo(){}
    foo<in Int>()
}
