// FIR_IDENTICAL
// ISSUE: KT-66313

val foo: String get() = ""

class Test1 {
    private val otherFoo = foo

    fun getFoo() = otherFoo
}

class Test2 {
    fun getFoo() = otherFoo

    private val otherFoo = foo
}
