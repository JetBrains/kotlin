package test

class AImpl(p: String) {
    fun foo(p: String) {}
    fun bar(p: String) {}
}

actual typealias A = AImpl
