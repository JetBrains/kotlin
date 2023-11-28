// FIR_IDENTICAL
// ISSUE: KT-63233

interface A {
    suspend fun foo()
}

<!ABSTRACT_MEMBER_NOT_IMPLEMENTED!>class B<!>: A {} //k1 - error, k2 - no error
