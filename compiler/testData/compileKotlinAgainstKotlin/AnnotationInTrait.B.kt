class C : a.Tr

fun main(args: Array<String>) {
    val method = C::class.java.getDeclaredMethod("foo")
    val annotations = method.getDeclaredAnnotations().joinToString("\n")
    if (annotations != "@a.Ann()") {
        throw AssertionError(annotations)
    }
}