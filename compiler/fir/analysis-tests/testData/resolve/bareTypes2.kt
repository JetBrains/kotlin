// RUN_PIPELINE_TILL: BACKEND
// LATEST_LV_DIFFERENCE
interface A<D : A<D>> {
    fun foo(): Any

    val cond: Boolean
    val field: Any
}

interface B<F : B<F>> : A<F> {
    override fun foo(): CharSequence
}

interface C : B<C> {
    override fun foo(): String
}

fun test(x: A<*>) {
    when ((x as? C)?.field) {
        is String -> {
            if ((x as? <!DEPRECATED_BARE_DOWNCAST!>B<!>)?.cond == true) {
                x.foo()
            }
        }
    }

}
