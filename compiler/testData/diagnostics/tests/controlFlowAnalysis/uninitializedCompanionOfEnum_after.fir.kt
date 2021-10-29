// LANGUAGE: +ProhibitAccessToEnumCompanionMembersInEnumConstructorCall
// ISSUE: KT-49110

enum class SomeEnum(val x: Int) {
    A(<!UNINITIALIZED_ENUM_COMPANION!>companionFun<!>().length),// UNINITIALIZED_ENUM_COMPANION
    B(<!UNINITIALIZED_VARIABLE!>companionProp<!>.length), // UNINITIALIZED_VARIABLE

    C(SomeEnum.<!UNINITIALIZED_ENUM_COMPANION!>companionFun<!>().length),
    D(<!UNINITIALIZED_VARIABLE!>SomeEnum.companionProp<!>.length),

    E(SomeEnum.<!UNINITIALIZED_ENUM_COMPANION!>Companion<!>.companionFun().length),
    F(<!UNINITIALIZED_VARIABLE!>SomeEnum.Companion.companionProp<!>.length); // UNINITIALIZED_VARIABLE

    companion object {
        val companionProp = "someString"
        fun companionFun(): String = "someString"
    }
}

enum class OtherEnum(val x: Int) {
    G(<!UNINITIALIZED_ENUM_COMPANION!>extensionFun<!>().length), // UNINITIALIZED_ENUM_COMPANION
    H(<!UNINITIALIZED_ENUM_COMPANION!>extensionProp<!>.length),

    I(<!UNINITIALIZED_ENUM_COMPANION!>OtherEnum<!>.extensionFun().length),
    J(<!UNINITIALIZED_ENUM_COMPANION!>OtherEnum<!>.extensionProp.length),

    K(OtherEnum.<!UNINITIALIZED_ENUM_COMPANION!>Companion<!>.extensionFun().length),
    L(OtherEnum.<!UNINITIALIZED_ENUM_COMPANION!>Companion<!>.extensionProp.length);

    companion object {
        val companionProp = "someString"
        fun companionFun(): String = "someString"
    }
}

fun OtherEnum.Companion.extensionFun(): String = companionFun()
val OtherEnum.Companion.extensionProp: String
    get() = companionProp

