trait P
trait R

trait A {
    fun foo(p: P): R
    fun dynamic(p: dynamic): dynamic
}

trait B : A {
    override fun foo(p: dynamic): dynamic
    override fun dynamic(p: P): R
}

trait A1 {
    fun foo(p: dynamic): dynamic
}

trait C : A, A1