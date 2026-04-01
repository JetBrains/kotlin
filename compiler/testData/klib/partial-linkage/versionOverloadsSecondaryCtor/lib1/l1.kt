class C {
    val a: Int
    constructor(a: Int = 1) { this.a = a }
    constructor(flag: Boolean) : this(2)
    fun foo(): String = "a=$a"
}