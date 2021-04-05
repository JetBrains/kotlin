package foo

actual sealed class SealedWithPlatformActuals <!ACTUAL_WITHOUT_EXPECT!>actual constructor()<!>: <!SEALED_INHERITOR_IN_DIFFERENT_MODULE!>SealedWithSharedActual<!>()
