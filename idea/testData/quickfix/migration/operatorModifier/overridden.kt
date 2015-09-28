// "Add 'operator' modifier" "false"
// ACTION: Convert to block body
// ACTION: Specify return type explicitly
open class A {
    open operator fun plus(a: A) = A()
}

class B : A() {
    override fun plu<caret>s(a: A) = A()
}
