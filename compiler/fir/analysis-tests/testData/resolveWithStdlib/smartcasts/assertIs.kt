import kotlin.test.*

fun foo(arg: Any) {
    assertIs<String>(arg, "")
    arg.length
}