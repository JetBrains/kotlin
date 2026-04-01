// RUN_PIPELINE_TILL: FRONTEND
// FULL_JDK
import java.util.function.Consumer

interface StringCollector : MutableList<String>

fun interface KConsumer<T> {
    fun accept(t: T)
}

fun foo(x: StringCollector.() -> Unit) {}
fun foo(x: Consumer<MutableList<String>>) {}

fun bar(x: StringCollector.() -> Unit) {}
fun bar(x: KConsumer<MutableList<String>>) {}

fun baz(x: StringCollector.() -> Unit) {}
fun baz(x: KConsumer<StringCollector>) {}

fun main() {
    foo { add("1") }
    bar { <!UNRESOLVED_REFERENCE!>add<!>("1") }
    baz { add("1") }
}

/* GENERATED_FIR_TAGS: funInterface, functionDeclaration, functionalType, interfaceDeclaration, lambdaLiteral,
nullableType, samConversion, stringLiteral, typeParameter, typeWithExtension */
