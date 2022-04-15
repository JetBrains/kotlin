class A {
    fun foo(): String = this.name

    val nameLength = this.foo().length
    val name = "Hello World"
}
