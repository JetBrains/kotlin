// FIR_IDENTICAL
// ISSUE: KT-59186

fun main() {
    <!CONFLICTING_OVERLOADS!>fun p () : Char<!> {
        return 'c'
    }

    <!CONFLICTING_OVERLOADS!>fun p () : Float<!> {
        return 13.0f
    }
}
