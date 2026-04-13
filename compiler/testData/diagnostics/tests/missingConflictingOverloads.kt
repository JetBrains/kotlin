// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-59186

fun main() {
    <!CONFLICTING_OVERLOADS!>fun p () : Char<!> {
        return 'c'
    }

    <!CONFLICTING_OVERLOADS!>fun p () : Float<!> {
        return 13.0f
    }
}

/* GENERATED_FIR_TAGS: functionDeclaration, localFunction */
