// CHOOSE_USE_SITE_TARGET: property

annotation class A

class Property {
    @A<caret>
    var foo: String = ""
}