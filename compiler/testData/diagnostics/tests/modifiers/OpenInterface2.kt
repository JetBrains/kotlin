// FIR_IDENTICAL
// ISSUE: KT-68724

interface A {
    fun foo()
}

<!REDUNDANT_MODIFIER_FOR_TARGET!>open<!> interface B : A {
}
