// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-75303
// WITH_STDLIB
// LANGUAGE: -EnableDfaWarningsInK2

class Foo

class Bar {
    fun render() = print(this)
}
val a = (Foo() as? Bar)?.render()
