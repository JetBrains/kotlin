// !WITH_NEW_INFERENCE
/*
 * RELEVANT SPEC SENTENCES (spec version: 0.1-152, test type: pos):
 *  - expressions, when-expression -> paragraph 2 -> sentence 1
 *  - expressions, when-expression, exhaustive-when-expressions -> paragraph 2 -> sentence 1
 *  - expressions, when-expression -> paragraph 9 -> sentence 1
 *  - expressions, conditional-expression -> paragraph 4 -> sentence 1
 *  - expressions, conditional-expression -> paragraph 5 -> sentence 1
 */

fun test1(): Int {
    val x: String = <!NI;TYPE_MISMATCH!>if (true) {
        when {
            true -> <!OI;TYPE_MISMATCH!>Any()<!>
            else -> <!OI;NULL_FOR_NONNULL_TYPE!>null<!>
        }
    } else ""<!>
    return x.hashCode()
}

fun test2(): Int {
    val x: String = <!NI;TYPE_MISMATCH, NI;TYPE_MISMATCH!>when {
                        true -> <!OI;TYPE_MISMATCH!>Any()<!>
                        else -> null
                    } ?: return 0<!>
    return x.hashCode()
}