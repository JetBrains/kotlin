// KT-2228

package test

trait A {
    val v: String
        get() = "test"
}