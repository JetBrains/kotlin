// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-69190

abstract class A {
    abstract val a: CharSequence
    protected abstract val b: CharSequence
}

sealed class B : A() {
    abstract override val a: String
    abstract override val b: String
}

class C : B() {
    override var a: String = ""
        private set  // This `private` is NOT unused

    override var b: String = ""
        private set  // This `private` is NOT unused
}
