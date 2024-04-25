// FIR_IDENTICAL
// DIAGNOSTICS: -UNUSED_PARAMETER -UNUSED_VARIABLE

class Inv<T>

fun <T> materialize(): T = TODO()

fun <A> foo(x: Inv<A>) {}
fun <B> bar(y: Inv<out B>): Inv<Inv<out B>> = materialize()

fun <K> test(plant: Inv<out K>) {
    val x = foo(bar(plant)) // OK in OI, NI: "Not enough information to infer type variable A"
}
