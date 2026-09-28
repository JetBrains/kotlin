// LANGUAGE: +ContextParameters +CallableReferencesToContextual

class WithCompanion {
    companion object {
        context(c: String)
        fun greet(arg: String): String = c + arg

        context(c: String)
        val decorated: String
            get() = "[$c]"
    }
}

fun box(): String {
    context("ctx:") {
        val g: (String) -> String = WithCompanion.Companion::greet
        if (g("arg") != "ctx:arg") return "FAIL 1: ${g("arg")}"

        val d = WithCompanion.Companion::decorated
        if (d.get() != "[ctx:]") return "FAIL 2: ${d.get()}"
    }
    return "OK"
}
