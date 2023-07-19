// FIR_IDENTICAL
// ISSUE: KT-59529
import kotlin.reflect.KProperty

interface Out<out F> {
    val x: F
}
interface Out2<out E> {
    val x: E
}
interface Inv<U> {
    val u: U
}

val outString: Out<String> = TODO()

val invOut2String by foo(outString)

operator fun <X> Inv<X>.getValue(
    thisRef: Any?,
    property: KProperty<*>
): Inv<X> = TODO()

fun <M> foo(other: Out<M>): Inv<Out2<M>> = TODO()

fun main() {
    invOut2String.u.x.length
}