// KT-23760
package foo

expect interface Upper

expect interface BaseI {
    fun f(p: Upper)
}

interface I : BaseI