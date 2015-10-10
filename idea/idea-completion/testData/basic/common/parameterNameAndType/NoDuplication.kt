import kotlin.properties.*

fun f(readonlypr<caret>)

// EXIST: { lookupString: "readOnlyProperty", itemText: "readOnlyProperty: ReadOnlyProperty", tailText: "<R, T> (kotlin.properties)" }
// NUMBER: 1
