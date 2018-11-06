// ALLOW_AST_ACCESS
package test

open class Parent {
    open fun foo(x: String) {}
}

open class KotlinClass : Parent() {
    override fun foo(x: String) {}
}
