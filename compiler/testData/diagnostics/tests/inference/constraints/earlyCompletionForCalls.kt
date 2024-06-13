// FIR_IDENTICAL
// DIAGNOSTICS: -UNUSED_PARAMETER

interface Parent
interface Inv<T>

object Child : Parent

fun <K : Parent> wrapper(): Inv<K> = TODO()
fun <T : Parent> consume(wrapper: Inv<T>) {}

fun <S> select(x: S, y: S): S = x

fun error(f: Inv<out Parent>, w: Inv<Child>) {
    consume(select(f, wrapper<Child>()))
}