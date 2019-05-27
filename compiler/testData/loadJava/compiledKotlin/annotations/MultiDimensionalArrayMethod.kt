// IGNORE_BACKEND: JVM_IR
//ALLOW_AST_ACCESS
package test

annotation class Anno(val s: String)

interface T {
    @Anno("foo")
    fun foo(): Array<Array<Array<T>>>

    @Anno("bar")
    val bar: Array<Array<BooleanArray>>
}
