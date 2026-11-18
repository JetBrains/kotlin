inline fun <reified T : Any> f() {
    println(object {
        override fun toString() = "o1 " + T::class.simpleName
    })
}
