fun foo() {
    val p = 1
    when (p) {
        0 -> System.out?.println()
        else -> System.out?.println()
    }
}
// 1 LINENUMBER 3
// Adding ignore flags below the test since the test relies on line numbers.
// IGNORE_BACKEND: JVM_IR
