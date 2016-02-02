// A.kt
package first

open class A {
    protected open fun test(): String = "FAIL (A)"
}

fun box() = second.C().value()