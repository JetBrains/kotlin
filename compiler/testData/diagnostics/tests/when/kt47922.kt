// ISSUE: KT-47922

package whencase.castissue

sealed class SealedBase {
    object Complete : SealedBase()
}

abstract class NonSealedBase {
    object Complete : NonSealedBase()
}

sealed class ToState

val sealedTest: SealedBase.() -> ToState? = {
    <!TYPE_MISMATCH!><!NON_EXHAUSTIVE_WHEN_STATEMENT!>when<!>(this) {}<!>
}

val nonSealedTest: NonSealedBase.() -> ToState? = {
    <!TYPE_MISMATCH!>when(this) {}<!>
}
