// KT-2228

package test

trait A {
    var v: String
        get() = "test"
        set(value) {
            throw UnsupportedOperationException()
        }
}