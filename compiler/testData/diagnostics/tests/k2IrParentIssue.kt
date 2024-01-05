// FIR_IDENTICAL
// ISSUE: KT-64089

fun <T> contentReturner(f: () -> T): T {
    return null <!UNCHECKED_CAST!>as T<!>
}

fun main(number: Int? = null) {
    contentReturner {
        "".apply {
            number!!
        }
    }

    number != null
}
