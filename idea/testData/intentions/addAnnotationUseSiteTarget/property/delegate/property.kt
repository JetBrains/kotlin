// CHOOSE_USE_SITE_TARGET: property
// WITH_RUNTIME

annotation class A

class Property {
    @A<caret>
    val foo: String by lazy { "" }
}