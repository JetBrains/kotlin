import kotlin.properties.*

fun f(keyMissin<caret>)

// EXIST: { lookupString: "keyMissingException", itemText: "keyMissingException: KeyMissingException", tailText: " (kotlin.properties)" }
// NUMBER: 1
