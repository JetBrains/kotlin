import kotlin.reflect.KProperty

inline operator fun String.getValue(t:Any?, p: KProperty<*>): String = p.name + this

object ForceOutOfOrder {
    fun callInline() = C.inlineFun()
}

object C {
    inline fun inlineFun() = {
        val O by "K"
        O
    }.let { it() }
}

fun box(): String = ForceOutOfOrder.callInline()
