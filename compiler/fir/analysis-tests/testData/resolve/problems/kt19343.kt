// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-19343
// WITH_STDLIB

// KT-19343: Bogus "assignment operators ambiguity" for int variable initialized using bitwise operations
class TestBogusAmbiguity {
    fun foo(bytes: ByteArray) {
        var x = bytes[0] <!UNRESOLVED_REFERENCE!>and<!> 0xFF
        x += 10
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, integerLiteral, localProperty, propertyDeclaration */
