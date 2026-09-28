fun named(f: (x: Int, y: String) -> Unit) {}

fun partiallyNamed(f: (x: Int, String) -> Unit) {}

fun unnamed(f: (Int, String) -> Unit) {}

fun withReceiver(f: String.(x: Int) -> Unit) {}

fun suspending(f: suspend (x: Int) -> Unit) {}

fun withContext(f: context(String) (x: Int) -> Unit) {}

fun nested(f: (g: (y: Int) -> Unit) -> Unit) {}

fun reflectType(f: kotlin.reflect.KFunction1<Int, Unit>) {}

val property: (count: Int) -> String
    get() = { "" }
