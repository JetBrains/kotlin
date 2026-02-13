fun invoke(f: () -> String): String = f()

val x: String get() = "2"

fun box(): String {
    fun localFun() = "1"
    if (invoke(::localFun) + invoke(::localFun) != "11") return "Fail 1"
    if (invoke(::x) + invoke(::x) != "22") return "Fail 2"
    return "OK"
}