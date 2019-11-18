// IGNORE_BACKEND_FIR: JVM_IR
interface Option<out T> {
    val s: String
}
class Some<T>(override val s: String) : Option<T>
class None(override val s: String = "None") : Option<Int>

fun whenTest(a: Int): Option<Any> = when (a) {
    239 ->  {
        if (a == 239) Some("239") else None()
    }
    else -> if (a != 239) Some("$a") else None()
}

fun ifTest(a: Int): Option<Any> = if (a == 239) {
    if (a == 239) Some("239") else None()
} else if (a != 239) Some("$a") else None()

fun box(): String {
    if (whenTest(2).s != "2") return "Fail 1"
    if (whenTest(239).s != "239") return "Fail 2"

    if (ifTest(2).s != "2") return "Fail 3"
    if (ifTest(239).s != "239") return "Fail 4"

    return "OK"
}