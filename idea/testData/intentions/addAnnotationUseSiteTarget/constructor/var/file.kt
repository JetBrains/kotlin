// CHOOSE_USE_SITE_TARGET: file
// IS_APPLICABLE: false

annotation class A

class Constructor(@A<caret> var foo: String)