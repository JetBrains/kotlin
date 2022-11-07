// ADDITIONAL_COMPILER_ARGUMENTS: -XXLanguage:+SafeExternalEnums
actual external enum class Foo: Enum<Foo> {
    A, B, C
}