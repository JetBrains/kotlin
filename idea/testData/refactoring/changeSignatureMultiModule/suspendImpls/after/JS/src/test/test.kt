package test

class C : I {
    suspend override fun foo(s: String, n: Int) = s.length
}