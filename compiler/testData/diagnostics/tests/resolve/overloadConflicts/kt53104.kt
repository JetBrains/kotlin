// FIR_IDENTICAL
// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-53104
// DIAGNOSTICS: -UNUSED_PARAMETER, -UNUSED_VARIABLE

open class Base
open class Sub : Base()
class BaseConfigurer(base: Base)
class SubConfigurer(sub: Sub)

fun configure(elem: Base, block: BaseConfigurer.() -> Unit) {}
fun configure(elem: Sub, block: SubConfigurer.() -> Unit) {}

fun main() {
    val sub: Sub = Sub()

    // Trailing lambda syntax — should resolve to Sub overload but gets OVERLOAD_RESOLUTION_AMBIGUITY
    <!OVERLOAD_RESOLUTION_AMBIGUITY!>configure<!>(sub) { }

    // Explicit lambda type — resolves correctly
    <!OVERLOAD_RESOLUTION_AMBIGUITY!>configure<!>(sub, {})

    // Non-lambda overloads for reference
    fun configureNoLambda(elem: Base) {}
    fun configureNoLambda(elem: Sub) {}
    configureNoLambda(sub)
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, functionalType, lambdaLiteral, localFunction,
localProperty, primaryConstructor, propertyDeclaration, typeWithExtension */
