// FILE: lib.kt
import kotlin.reflect.KProperty

inline operator fun String.getValue(t:Any?, p: KProperty<*>): String = p.name + this

object C {
    inline fun inlineFun() = {
        val O by "K"
        O
    }.let { it() }
}

// FILE: main.kt
import kotlin.reflect.KProperty

object ForceOutOfOrder {
    fun callInline() = C.inlineFun()
}

fun box(): String = ForceOutOfOrder.callInline()
