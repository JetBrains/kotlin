package test

expect class A(p: String = "constructor") {
    fun foo(p: String = "common")
    fun bar(p: String)
}
