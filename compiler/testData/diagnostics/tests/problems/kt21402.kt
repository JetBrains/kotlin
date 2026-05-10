// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-21402

// KT-21402: Wrong overload resolution ambiguity when argument is lambda and parameter is type parameter with not function upper bound
fun main(args: Array<String>) {
    Foo {}
}

abstract class Table
object Foo {
    operator fun <T : Table> invoke(table: T) {}
    operator fun <R> invoke(block: Foo.() -> R): R = block()
}
/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, functionalType, lambdaLiteral, nullableType,
objectDeclaration, operator, typeConstraint, typeParameter, typeWithExtension */
