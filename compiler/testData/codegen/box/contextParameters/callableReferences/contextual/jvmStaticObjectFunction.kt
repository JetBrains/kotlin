// LANGUAGE: +ContextParameters +CallableReferencesToContextual
// TARGET_BACKEND: JVM_IR
// WITH_STDLIB

object Obj {
    @JvmStatic
    context(c: String)
    fun greet(arg: String): String = c + arg
}

class WithCompanion {
    companion object {
        @JvmStatic
        context(c: String)
        fun greet(arg: String): String = c + arg
    }
}

fun box(): String = context("ctx:") {
    val o: (String) -> String = Obj::greet
    if (o("arg") != "ctx:arg") return@context "FAIL 1: ${o("arg")}"

    val c: (String) -> String = WithCompanion.Companion::greet
    if (c("arg") != "ctx:arg") return@context "FAIL 2: ${c("arg")}"

    "OK"
}
