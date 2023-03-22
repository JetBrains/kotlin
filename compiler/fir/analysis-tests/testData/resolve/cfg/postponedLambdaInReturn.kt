// IGNORE_REVERSED_RESOLVE
// !DUMP_CFG

import kotlin.contracts.*

class Lateinit<R : Any> {
    lateinit var value: R
}

@OptIn(ExperimentalContracts::class)
public inline fun <R : Any> build(crossinline builder: Lateinit<R>.() -> Unit): R {
    contract { callsInPlace(builder, InvocationKind.EXACTLY_ONCE) }
    return Lateinit<R>().apply(builder).value
}

val p = false

fun test1() {
    var y: String? = null
    val x: String = run {
        if (p)
            return@run build { y as String; value = "..." }
        else
            return@run ""
    }
    y<!UNSAFE_CALL!>.<!>length // bad
}

fun test2() {
    val x: String = run {
        while (true) {
            try {
                return@run build { value = "..." }
            } catch (e: Throwable) {}
        }
        throw Exception()
    }
    x.length
}

fun test3() {
    var y: String?
    y = ""
    val x: String = run {
        if (!p)
            return@run build { y = null; value = "..." }
        else
            return@run ""
    }
    y.length // bad
}
