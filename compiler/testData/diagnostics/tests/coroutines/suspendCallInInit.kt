// FIR_IDENTICAL
suspend fun foo() {}

suspend fun test() {
    class Foo {
        init {
            <!NON_LOCAL_SUSPENSION_POINT!>foo<!>()
        }

        val prop = <!NON_LOCAL_SUSPENSION_POINT!>foo<!>()
    }
}
