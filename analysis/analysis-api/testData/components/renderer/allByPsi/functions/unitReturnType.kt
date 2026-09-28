fun implicitUnit() {}

fun explicitUnit(): Unit {}

// A nullable `Unit` is not an implicit return type, so it should be rendered.
fun nullableUnit(): Unit? = null

suspend fun suspendingUnit() {}
