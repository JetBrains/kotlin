// !DIAGNOSTICS: -CAST_NEVER_SUCCEEDS

interface I

interface Inv<P>
interface Out<out T>

class Bar<U : I>(val x: Inv<Out<U>>)

fun <T> materializeFoo(): Inv<T> = null as Inv<T>

fun main() {
    Bar(materializeFoo())
}