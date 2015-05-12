// KT-2228

package test

interface A {
    val v: String
        get() = "test"
}