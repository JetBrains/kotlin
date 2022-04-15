class A {
    fun foo(): String = this.name

    val nameLength = foo().length
    val name = "Hello World"
}
