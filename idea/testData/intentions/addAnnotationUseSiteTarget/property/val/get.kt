// CHOOSE_USE_SITE_TARGET: get

annotation class A

class Property {
    @A<caret>
    val foo: String = ""
}