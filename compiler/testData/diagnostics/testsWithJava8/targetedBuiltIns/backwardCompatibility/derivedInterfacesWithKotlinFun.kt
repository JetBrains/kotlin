// !DIAGNOSTICS: -UNUSED_PARAMETER -PLATFORM_CLASS_MAPPED_TO_KOTLIN

interface IBaseWithKotlinDeclaration : Map<String, String> {
    fun replace(key: String, value: String): String?
}

interface TestDerivedInterfaceHidingWithKotlinDeclaration : IBaseWithKotlinDeclaration {
    // VIRTUAL_MEMBER_HIDDEN: hides member declaration inherited from a Kotlin interface
    <!VIRTUAL_MEMBER_HIDDEN!>fun replace(key: String, value: String): String?<!>
}

interface TestDerivedInterfaceDefaultWithKotlinDeclaration : IBaseWithKotlinDeclaration {
    // VIRTUAL_MEMBER_HIDDEN: hides member declaration inherited from a Kotlin interface
    <!VIRTUAL_MEMBER_HIDDEN!>fun replace(key: String, value: String): String?<!> = TODO()
}
