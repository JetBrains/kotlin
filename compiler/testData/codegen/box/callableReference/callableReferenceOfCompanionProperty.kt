import kotlin.reflect.KProperty0

class Sample {
    companion object {
        val maxValue = 1
    }
}

abstract class Checker {
    fun check(): String {
        return run(
            Sample::maxValue,
            { x -> x == 1 }
        )
    }
    abstract fun <T1> run(method: KProperty0<T1>, fn: (T1) -> Boolean): String
}

fun box(): String {
    var result = ( object : Checker() {
        override fun <T1> run(method: KProperty0<T1>, fn: (T1) -> Boolean): String {
            return "OK"
        }
    } ).check()

    return result
}
