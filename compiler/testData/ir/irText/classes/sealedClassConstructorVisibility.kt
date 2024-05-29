// SEPARATE_SIGNATURE_DUMP_FOR_K2
// ^ KT-68617

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
