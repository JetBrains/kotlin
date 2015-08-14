import lib.KotlinClass

fun test() = KotlinClass().foo(<caret>)

// ABSENT: p0
// EXIST: { lookupString:"paramName", itemText:"paramName =" }
