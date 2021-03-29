// "Remove redundant 'open' modifier" "true"
abstract class B() {
    abstract fun foo()
}

abstract class A() : B() {
    abstract override <caret>open fun foo()
}

