// IGNORE_BACKEND_FIR: JVM_IR
interface Id<T> {
    val id: T
}

data class Actor (
        override val id: Int,
        val firstName: String,
        val lastName: String
) : Id<Int>

fun box(): String {
    val a1 = Actor(1, "Jeff", "Bridges")

    val a1c = a1.copy()
    if (a1c.id != a1.id) return "Failed: a1.copy().id==${a1c.id}"

    val a2 = Actor(2, "Jeff", "Bridges")
    if (a2 == a1) return "Failed: a2==a1"

    // Assume that our hashCode is good enough for this test :)
    if (a2.hashCode() == a1.hashCode()) return "Failed: a2.hashCode()==a1.hashCode()"

    a1.toString()

    return "OK"
}
