// WITH_STDLIB

import kotlin.reflect.KFunction1

fun len(s: String): Int = s.length

fun box(): String {
    val f: (String) -> Int = ::len
    val k: KFunction1<String, Int> = ::len

    if (f("OK") != 2) return "FAIL f: ${f("OK")}"
    if (k("OK") != 2) return "FAIL k: ${k("OK")}"
    if (k.name != "len") return "FAIL name: ${k.name}"

    return "OK"
}
