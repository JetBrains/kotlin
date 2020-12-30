// !WITH_NEW_INFERENCE
/*
 * KOTLIN DIAGNOSTICS SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-152
 * PRIMARY LINKS: expressions, when-expression -> paragraph 2 -> sentence 1
 * expressions, when-expression, exhaustive-when-expressions -> paragraph 2 -> sentence 1
 * expressions, when-expression -> paragraph 9 -> sentence 1
 * expressions, conditional-expression -> paragraph 4 -> sentence 1
 * expressions, conditional-expression -> paragraph 5 -> sentence 1
 */

fun test1(): Int {
    val x: String = if (true) <!TYPE_MISMATCH{NI}!>{
        when {
            true -> <!TYPE_MISMATCH{OI}!>Any()<!>
            else -> <!NULL_FOR_NONNULL_TYPE{OI}!>null<!>
        }
    }<!> else ""
    return x.hashCode()
}

fun test2(): Int {
    val x: String = <!TYPE_MISMATCH{NI}, TYPE_MISMATCH{NI}!>when {
                        true -> <!TYPE_MISMATCH{OI}!>Any()<!>
                        else -> null
                    } ?: return 0<!>
    return x.hashCode()
}
