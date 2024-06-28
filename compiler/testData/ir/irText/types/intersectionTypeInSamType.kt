// FIR_IDENTICAL
// SKIP_KT_DUMP
// IGNORE_BACKEND_K1: JS_IR

// KT-61141: difference in IR for implicit cast
// IGNORE_BACKEND_K1: NATIVE
interface X
interface Z

interface A : X, Z
interface B : X, Z

fun interface IFoo<T : X> {
    fun foo(t: T)
}

fun interface IBar1<T> where T : X, T : Z {
    fun bar(t: T)
}

fun interface IBar2<T> where T : X, T : Z {
    fun bar(t: T)
}

fun <T> sel(x: T, y: T) = x

class G1<T : X> {
    fun checkFoo(x: IFoo<in T>) {}
}

class G2<T> where T : X, T : Z {
    fun checkFoo(x: IFoo<in T>) {}
    fun checkBar1(x: IBar1<in T>) {}
    fun checkBar2(x: IBar2<in T>) {}
}


fun test1() {
    val g = sel(G1<A>(), G1<B>())
    g.checkFoo {}
}

fun test2() {
    val g = sel(G2<A>(), G2<B>())
    g.checkFoo {}
    g.checkBar1 {}
    g.checkBar2 {}
}
