// "Remove redundant 'open' modifier" "true"
abstract class B() {
    abstract fun foo()
}

abstract class A() : B() {
    abstract <caret>open override fun foo()
}

/* FIR_COMPARISON */