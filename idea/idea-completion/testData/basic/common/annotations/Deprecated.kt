@Dep<caret>
fun foo() { }

// INVOCATION_COUNT: 1
// EXIST: { itemText: "Deprecated", tailText: " (kotlin)" }
// EXIST: { itemText: "DeprecatedSinceKotlin", tailText: " (kotlin)" }
// NOTHING_ELSE
