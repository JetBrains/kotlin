fun <T> eval(f: () -> T) = f()

class A {
    private var foo = 1

    fun `access$getFoo$p`(a: A): Int = 1
    fun `access$setFoo$p`(a: A, d: Int) {}

    //companion backing field accessors
    fun `access$getFoo$cp`(): Int = 1
    fun `access$setFoo$cp`(d: Int) {}

    val bar = 0
        get() = { field }()

    //synthetic field convention accessor
    fun `access$getBar$lp`(a: A): Int = 7

    companion object {
        private var foo = 1
            // Custom getter is needed, otherwise no need to generate getY and setY
            get() = field

        fun test() {
            eval {
                foo = 2
                foo
            }
        }

        fun `access$getFoo$p`(p: A.Companion): Int = 1
        fun `access$setFoo$p`(p: A.Companion, d: Int) {}
    }

    fun test() {
        eval {
            foo = 2;
            foo + bar
        }
    }
}