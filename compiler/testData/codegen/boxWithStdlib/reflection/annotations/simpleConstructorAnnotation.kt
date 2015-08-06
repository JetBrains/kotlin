annotation class Primary
annotation class Secondary

class C @Primary constructor() {
    @Secondary
    constructor(s: String): this()
}

fun box(): String {
    val ans = C::class.constructors.map { it.annotations.single().annotationType().simpleName }.toSortedList()
    if (ans != listOf("Primary", "Secondary")) return "Fail: $ans"
    return "OK"
}
