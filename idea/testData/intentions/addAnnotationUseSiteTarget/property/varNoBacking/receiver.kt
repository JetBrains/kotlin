// CHOOSE_USE_SITE_TARGET: receiver
// IS_APPLICABLE: false

annotation class A

class Property {
    @A<caret>
    var foo: String
        get() = ""
        set(p) {}
}