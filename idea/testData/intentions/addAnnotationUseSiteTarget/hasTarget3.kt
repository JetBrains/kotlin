// CHOOSE_USE_SITE_TARGET: get

annotation class A

annotation class B

class Test {
    @get:B
    @A<caret>
    val foo: String = ""
}