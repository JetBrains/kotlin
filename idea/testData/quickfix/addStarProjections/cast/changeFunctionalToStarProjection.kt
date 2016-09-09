// "Change type arguments to <*, *>" "false"
// ACTION: Convert to expression body

class Constructor<out T>(val x: T)

fun y() : Constructor<*> = Constructor(42)
fun x(): (String) -> String {
    return y() as<caret> (String) -> String
}