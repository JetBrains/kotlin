// LANGUAGE: +ContextParameters +CallableReferencesToContextual
// TARGET_BACKEND: JVM
// WITH_STDLIB

@JvmInline
value class Z(val value: String)

context(z: Z)
fun render(suffix: String): String = z.value + suffix

context(z: Z)
val decorated: String
    get() = "[" + z.value + "]"

fun box(): String {
    context(Z("O")) {
        val f: (String) -> String = ::render
        if (f("K") != "OK") return "FAIL 1: ${f("K")}"

        val p = ::decorated
        if (p.get() != "[O]") return "FAIL 2: ${p.get()}"
    }

    val r1 = context(Z("A")) { val r: (String) -> String = ::render; r }
    val r2 = context(Z("B")) { val r: (String) -> String = ::render; r }
    val r3 = context(Z("A")) { val r: (String) -> String = ::render; r }

    if (r1 == r2) return "FAIL 3: references capturing different value-class context arguments compare equal"
    if (r1 != r3) return "FAIL 4: references capturing equal value-class context arguments compare unequal"
    if (r1.hashCode() != r3.hashCode()) return "FAIL 5: equal references have different hashCodes"

    return "OK"
}
