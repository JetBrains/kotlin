// DECLARATION_TYPE: org.jetbrains.kotlin.psi.KtEnumEntry
// MAIN_FILE_NAME: FooBar
annotation class Anno(val value: String)

enum class FooBar {
    @Anno("baz")
    Baz;
}
