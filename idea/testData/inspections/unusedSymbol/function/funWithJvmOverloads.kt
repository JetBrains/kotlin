import kotlin.jvm.JvmOverloads

@JvmOverloads
fun foo(s: String = "")

fun main(args: Array<String>) {
    foo()
}