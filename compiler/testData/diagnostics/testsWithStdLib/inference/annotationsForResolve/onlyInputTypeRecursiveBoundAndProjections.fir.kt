// !DIAGNOSTICS: -UNUSED_PARAMETER

@file:Suppress("INVISIBLE_MEMBER", <!ERROR_SUPPRESSION!>"INVISIBLE_REFERENCE"<!>)

interface IFace<K, out V>

fun <@kotlin.internal.OnlyInputTypes K, V> IFace<out K, V>.get(key: K): V? = TODO()
fun <@kotlin.internal.OnlyInputTypes I> id(arg: I): I = arg

interface InvBase<B>
class DerivedInv : InvBase<DerivedInv>
class InvRecursive<E : InvBase<E>>

fun test1(argument: InvRecursive<*>, receiver: IFace<InvRecursive<DerivedInv>, Any>) {
    receiver.get(argument)
}

fun test2(arg: InvRecursive<out DerivedInv>) {
    id(arg)
}

fun test3(arg: InvRecursive<in DerivedInv>) {
    id(arg)
}
