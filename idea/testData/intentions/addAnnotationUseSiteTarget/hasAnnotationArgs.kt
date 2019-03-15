// CHOOSE_USE_SITE_TARGET: field

annotation class A(val s: String)

class Test {
    @A("...")<caret>
    val foo: String = ""
}