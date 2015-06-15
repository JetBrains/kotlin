interface Id<T> {
    val id: T
}

open data class Actor (
        id: Int,
        val firstName: String,
        val lastName: String
) : Id<Int> {
    override val id: Int = id
}

fun box(): String {
    val a1 = Actor(1, "Jeff", "Bridges")
    val a1copy = a1.copy(id = a1.id)

    if (a1copy.id != a1.id) return "Failed: a1copy.id==${a1copy.id}"

    return "OK"
}