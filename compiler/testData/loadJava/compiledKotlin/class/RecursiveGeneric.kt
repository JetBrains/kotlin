//ALLOW_AST_ACCESS
package test

trait Rec<R, out T: Rec<R, T>> {
    fun t(): T
}

trait Super {
    fun foo(p: Rec<*, *>) = p.t()
}