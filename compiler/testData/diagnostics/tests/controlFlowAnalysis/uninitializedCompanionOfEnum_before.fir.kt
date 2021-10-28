// LANGUAGE: -ProhibitAccessToEnumCompanionMembersInEnumConstructorCall
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
    G(extensionFun().length), // UNINITIALIZED_ENUM_COMPANION
    H(extensionProp.length),

    I(OtherEnum.extensionFun().length),
    J(OtherEnum.extensionProp.length),

    K(OtherEnum.Companion.extensionFun().length),
    L(OtherEnum.Companion.extensionProp.length);

    companion object {
        val companionProp = "someString"
        fun companionFun(): String = "someString"
    }
}

fun OtherEnum.Companion.extensionFun(): String = companionFun()
val OtherEnum.Companion.extensionProp: String
    get() = companionProp

