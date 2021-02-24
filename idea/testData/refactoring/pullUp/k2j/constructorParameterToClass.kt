// WITH_RUNTIME
abstract class <caret>B(
    // INFO: {"checked": "true"}
    val n: Int,
    // INFO: {"checked": "true"}
    val s: String,
    val b: Boolean
): A()