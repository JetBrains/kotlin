//ALLOW_AST_ACCESS
package test

data class A(foo: String, val bar: Int, other: Long) {
    val foo = foo
    val other = other
}