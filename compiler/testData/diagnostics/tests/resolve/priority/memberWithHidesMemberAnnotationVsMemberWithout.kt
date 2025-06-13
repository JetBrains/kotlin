// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
// WITH_STDLIB
// ISSUE: KT-50776
interface Entities<Target> : MutableCollection<Target>, Sequence<Target>

abstract class StringEntities : Entities<String> {
    fun foo() {
        forEach {
            println(it)
        }
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, interfaceDeclaration, lambdaLiteral, nullableType,
typeParameter */
