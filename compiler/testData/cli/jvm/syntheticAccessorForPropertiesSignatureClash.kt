class A {

    private var foo = 1;

    fun `access$getFoo$p`(a: A): Int = 1
    fun `access$setFoo$p`(a: A, d: Int) {}

    //companion backing field accessors
    fun `access$getFoo$cp`(): Int = 1
    fun `access$setFoo$cp`(d: Int) {}

    companion object {
        private var foo = 1;

        fun test() {
            {
                foo = 2;
                foo
            }()
        }

        fun `access$getFoo$p`(p: A.Companion): Int = 1
        fun `access$setFoo$p`(p: A.Companion, d: Int) {}
    }

    fun test() {
        {
            foo = 2;
            foo
        }()
    }
}