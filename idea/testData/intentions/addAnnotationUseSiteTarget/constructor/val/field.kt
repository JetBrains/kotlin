// CHOOSE_USE_SITE_TARGET: field

annotation class A

class Constructor(@A<caret> val foo: String)