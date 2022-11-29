// LANGUAGE: -ProhibitAccessToEnumCompanionMembersInEnumConstructorCall
// ISSUE: KT-49110, KT-54055

enum class SomeEnum(val x: Int) {
    A(<!UNINITIALIZED_ENUM_COMPANION_WARNING!><!UNINITIALIZED_ENUM_COMPANION!>companionFun<!>()<!>.length),// UNINITIALIZED_ENUM_COMPANION
    B(<!UNINITIALIZED_ENUM_COMPANION_WARNING, UNINITIALIZED_VARIABLE!>companionProp<!>.length), // UNINITIALIZED_VARIABLE

    C(<!UNINITIALIZED_ENUM_COMPANION_WARNING!>SomeEnum<!>.companionFun().length),
    D(<!UNINITIALIZED_ENUM_COMPANION_WARNING!>SomeEnum<!>.companionProp.length),

    E(SomeEnum.<!UNINITIALIZED_ENUM_COMPANION_WARNING!>Companion<!>.companionFun().length),
    F(SomeEnum.<!UNINITIALIZED_ENUM_COMPANION_WARNING!>Companion<!>.<!UNINITIALIZED_VARIABLE!>companionProp<!>.length); // UNINITIALIZED_VARIABLE

    companion object {
        val companionProp = "someString"
        fun companionFun(): String = "someString"
    }
}

enum class OtherEnum(val x: Int) {
    G(<!UNINITIALIZED_ENUM_COMPANION_WARNING!><!UNINITIALIZED_ENUM_COMPANION!>extensionFun<!>()<!>.length), // UNINITIALIZED_ENUM_COMPANION
    H(<!UNINITIALIZED_ENUM_COMPANION_WARNING!>extensionProp<!>.length),

    I(<!UNINITIALIZED_ENUM_COMPANION_WARNING!>OtherEnum<!>.extensionFun().length),
    J(<!UNINITIALIZED_ENUM_COMPANION_WARNING!>OtherEnum<!>.extensionProp.length),

    K(OtherEnum.<!UNINITIALIZED_ENUM_COMPANION_WARNING!>Companion<!>.extensionFun().length),
    L(OtherEnum.<!UNINITIALIZED_ENUM_COMPANION_WARNING!>Companion<!>.extensionProp.length);

    companion object {
        val companionProp = "someString"
        fun companionFun(): String = "someString"
    }
}

fun OtherEnum.Companion.extensionFun(): String = companionFun()
val OtherEnum.Companion.extensionProp: String
    get() = companionProp

enum class EnumWithLambda(val lambda: () -> Unit) {
    M({
      <!UNINITIALIZED_ENUM_COMPANION_WARNING!>companionFun()<!>.length
      <!UNINITIALIZED_ENUM_COMPANION_WARNING!>companionProp<!>.length

      <!UNINITIALIZED_ENUM_COMPANION_WARNING!>EnumWithLambda<!>.companionFun().length
      <!UNINITIALIZED_ENUM_COMPANION_WARNING!>EnumWithLambda<!>.companionProp.length

      <!UNINITIALIZED_ENUM_COMPANION_WARNING!>extensionFun()<!>.length
      <!UNINITIALIZED_ENUM_COMPANION_WARNING!>extensionProp<!>.length

      <!UNINITIALIZED_ENUM_COMPANION_WARNING!>EnumWithLambda<!>.extensionFun().length
      <!UNINITIALIZED_ENUM_COMPANION_WARNING!>EnumWithLambda<!>.extensionProp.length
      });

    companion object {
        val companionProp = "someString"
        fun companionFun(): String = "someString"
    }
}

fun EnumWithLambda.Companion.extensionFun(): String = companionFun()
val EnumWithLambda.Companion.extensionProp: String
    get() = companionProp

