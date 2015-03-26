class A {
    constructor()
    fun foo() = 1
    public constructor()
    val x = 2

    constructor(): this()
    constructor(): super(3)
    val x = 4
}
