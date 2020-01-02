fun <A : Array<Any>> f1() {}
fun <T, A : Array<out T>> f2() {}
fun <S, T : S, A> f3() where A : Array<out S>, A : Array<out T> {}

fun <T : IntArray> f4() {}

fun <T> f5() where T : Array<Any> {}

val <T : Array<Any?>> T.v: String get() = ""

class C2<T, A : Array<out T>>
interface C3<S, T : S, A> where A : Array<out S>, A : Array<out T>

fun foo() {
    class C1<A : Array<Any>> {
        fun <A : Array<Any>, B : Array<Any>, C : A> f() {}
    }
}
