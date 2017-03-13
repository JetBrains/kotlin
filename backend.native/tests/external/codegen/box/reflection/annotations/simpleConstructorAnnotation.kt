// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS, NATIVE

// WITH_REFLECT

annotation class Primary
annotation class Secondary

class C @Primary constructor() {
    @Secondary
    constructor(s: String): this()
}

fun box(): String {
    val ans = C::class.constructors.map { it.annotations.single().annotationClass.java.simpleName }.sorted()
    if (ans != listOf("Primary", "Secondary")) return "Fail: $ans"
    return "OK"
}
