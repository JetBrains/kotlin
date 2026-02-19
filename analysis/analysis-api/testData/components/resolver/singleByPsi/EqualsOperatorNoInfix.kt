package test

class A(val n: Any) {
    override fun equals(other: Any?): Boolean = other is A && other.n <caret>== n
}