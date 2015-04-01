//ALLOW_AST_ACCESS
package test

annotation class Anno(val s: String)

trait T {
    Anno("foo")
    fun foo(): Array<Array<Array<T>>>

    Anno("bar")
    val bar: Array<Array<BooleanArray>>
}
