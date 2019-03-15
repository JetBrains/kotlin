// CHOOSE_USE_SITE_TARGET: set
// IS_APPLICABLE: false

annotation class A

class Constructor(@A<caret> val foo: String)