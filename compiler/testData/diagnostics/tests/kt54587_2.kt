// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL
package one

fun test(f: NextMissing) {
    for(i in <!NEXT_NONE_APPLICABLE!>f<!>) {} //[NEXT_NONE_APPLICABLE] is expected as in K1
}

interface Doo
operator fun Doo.next() {}

interface NextMissing {
    operator fun iterator(): NextMissing2
}

interface NextMissing2 {
    operator fun hasNext(): Boolean
}
