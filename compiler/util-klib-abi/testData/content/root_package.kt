// Check how declarations from the root package are rendered.

public class Class(val property: String) {
    fun function(): String = ""
    class NestedClass
}

fun function(): String = ""

val property: String get() = ""
