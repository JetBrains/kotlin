// IS_APPLICABLE: false
// ERROR: 'infix' modifier is inapplicable on this function: must be a member or an extension function
infix fun id(s: String) = s
val x = <caret>id("0").get(0)
