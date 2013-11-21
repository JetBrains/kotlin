trait A {
    public val c: Int
}

trait B: A {
    override protected private val c: Int
}
