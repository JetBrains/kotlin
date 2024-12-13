interface A {
    public val c: Int
}

interface B: A {
    override protected private val c: Int
}
