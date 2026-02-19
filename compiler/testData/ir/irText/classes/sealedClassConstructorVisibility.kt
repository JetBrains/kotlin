// ISSUE: KT-68617
// FIR_IDENTICAL

sealed class UnspecifiedPrimary()
sealed class PrivatePrimary private constructor()
sealed class ProtectedPrimary protected constructor()

sealed class UnspecifiedSecondary() {
    constructor(i: Int) : this()
}

sealed class PrivateSecondary() {
    private constructor(i: Int) : this()
}

sealed class ProtectedSecondary() {
    protected constructor(i: Int) : this()
}
