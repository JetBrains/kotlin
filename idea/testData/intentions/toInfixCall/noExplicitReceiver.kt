// IS_APPLICABLE: false
infix fun id(s: String) = s
val x = <caret>id("0").get(0)