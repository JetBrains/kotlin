interface P
interface R

interface A {
    fun foo(p: P): R
    fun dynamic(p: dynamic): dynamic
}

interface B : A {
    override fun foo(p: dynamic): dynamic
    override fun dynamic(p: P): R
}

interface A1 {
    fun foo(p: dynamic): dynamic
}

interface C : A, A1