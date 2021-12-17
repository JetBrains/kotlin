// WITH_STDLIB
// For FIR: see KT-50293

fun main() {
    val list = buildList {
        add("one")
        add("two")

        val secondParameter = get(1)
        println(secondParameter as String) // WARNING: [CAST_NEVER_SUCCEEDS] This cast can never succeed
    }
}