import kotlin.properties.*

fun f(readonlypr<caret>)

// EXIST: { itemText: "readOnlyProperty: ReadOnlyProperty", tailText: "<R, T> (kotlin.properties)" }
// NUMBER: 1
