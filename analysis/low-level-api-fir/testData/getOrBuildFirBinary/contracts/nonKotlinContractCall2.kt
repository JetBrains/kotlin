// DECLARATION_TYPE: org.jetbrains.kotlin.psi.KtClass
// MAIN_FILE_NAME: Main
// FILE: Test.kt
class Test

// FILE: Main.kt
class Main {
    fun testBuilder(id: String = "", lambda: Test.() -> Unit) = Test()

    fun test() {
        contract(testBuilder {})
    }

    fun contract(test: Test) {}
}