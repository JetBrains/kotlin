// "Add non-null asserted (!!) call" "true"
// ACTION: Cast expression 'a' to 'Foo'

interface Foo {
    fun bar()
}

open class MyClass {
    open val a: Foo? = null

    fun foo() {
        if (a != null) {
            <caret>a.bar()
        }
    }
}
// TODO: Enable when FIR reports SMARTCAST_IMPOSSIBLE
/* IGNORE_FIR */
