class A {

    private var foo = 1;

    fun `access$getFoo$p`(a: A): Int = 1
    fun `access$setFoo$p`(a: A, d: Int) {}

    private fun getFoo() = 1;
    private fun setFoo(i: Int) {}

    fun `access$getFoo`(a: A): Int = 1
    fun `access$setFoo`(a: A, d: Int) {}

    fun test() {
        {
            foo = 2;
            foo
        }();

        {
            setFoo(2)
            getFoo();
        }()
    }
}