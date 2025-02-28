// WITH_STDLIB
// ISSUE: KT-67520
// LANGUAGE: +AvoidWrongOptimizationOfTypeOperatorsOnValueClasses

inline class X(val x: String)

fun box(): String {
    val res1 = runCatching {
        val cmp: Comparable<String> = leakInline<String>(X(""))
    }
    require(res1.exceptionOrNull()?.message == "Alas") { res1.toString() }

    val res2 = runCatching {
        val cmp: Comparable<String> = leakInline<String>(X(""))
        "Hey " + cmp
    }
    require(res2.exceptionOrNull()?.message == "Alas") { res2.toString() }
    
    return "OK"
}


inline fun <T> leakInline(a: Any): Comparable<T> {
    if (a is Comparable<*>) return a as Comparable<T>
    error("Alas")
}
