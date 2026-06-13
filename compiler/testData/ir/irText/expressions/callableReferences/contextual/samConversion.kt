// LANGUAGE: +ContextParameters +CallableReferencesToContextual

fun interface Sam {
    fun invoke()
}

context(_: String)
fun foo() {}

fun acceptSam(t: Sam) {}

fun String.test() {
    acceptSam(::foo)
}
