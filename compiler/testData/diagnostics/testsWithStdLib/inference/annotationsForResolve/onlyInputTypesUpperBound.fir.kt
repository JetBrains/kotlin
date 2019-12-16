// !DIAGNOSTICS: -UNUSED_PARAMETER -UNUSED_VARIABLE
// !WITH_NEW_INFERENCE

@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

class Inv<T>
class Out<out T>
fun <T> foo(i: Inv<in T>, o: Out<T>) {
    bar(i, o)
}

fun <@kotlin.internal.OnlyInputTypes K> bar(r: Inv<out K>, o: Out<K>): K = TODO()
