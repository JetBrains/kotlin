// MEMBER_CLASS_FILTER: org.jetbrains.kotlin.fir.symbols.impl.FirConstructorSymbol
package one

interface B<T> {
    fun f() = true
}

open class A<T>(b: Boolean)

interface D

class C : B<Int> {
    inner class Inn<caret>er : A<D>(super<B>.f())
}
