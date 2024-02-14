// ISSUE: KT-65101
// WITH_STDLIB
interface Context<C: Context<C>>
interface InterfaceA<C: Context<C>>

class ABuilder<C: Context<C>, A: InterfaceA<C>, B: InterfaceA<C>>

operator fun <C, A, B> ABuilder<C, A, B>.invoke(block: B.() -> Unit): Unit
        where C: Context<C>,
              A: InterfaceA<C>,
              B: InterfaceA<C>
        = null!!

object ContextImpl : Context<ContextImpl>
object RootA : InterfaceA<ContextImpl>

class AImpl<P: Context<P>> : InterfaceA<P> {
    fun foo(): Int = null!!
}

val <C: Context<C>, A: InterfaceA<C>> A.impl get() = ABuilder<C, A, AImpl<C>>()

fun test_1() {
    RootA.apply {
        (impl) {
            foo()
        }
    }
}

fun test_2() {
    RootA.apply {
        impl {
            foo()
        }
    }
}
