interface I {
    fun f()
}

expect class C : I {
    override fun f()
}