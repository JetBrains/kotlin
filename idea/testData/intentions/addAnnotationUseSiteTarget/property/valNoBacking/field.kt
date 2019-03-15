// CHOOSE_USE_SITE_TARGET: field
// IS_APPLICABLE: false

annotation class A

class Property {
    @A<caret>
    val foo: String
        get() = ""
}