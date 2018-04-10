// CHOOSE_USE_SITE_TARGET: file
// IS_APPLICABLE: false

annotation class A

class Property {
    @A<caret>
    var foo: String = ""
}