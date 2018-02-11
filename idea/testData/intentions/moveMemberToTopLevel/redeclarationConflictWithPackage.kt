// SHOULD_FAIL_WITH: Package 'foo.bar' already contains function f1(Int)
package foo.bar

object Test {
    fun <caret>f1(n: Int) {}
}

fun f1(n: Int) {}
