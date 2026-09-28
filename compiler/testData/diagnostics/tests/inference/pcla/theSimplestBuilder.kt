// RUN_PIPELINE_TILL: CODEGEN
interface Box<F> {
    fun add(f: F)
    fun get(): F
}

fun <E> myBuilder(x: Box<E>.() -> Unit): Box<E> = TODO()

fun main() {
    myBuilder {
        add("")
    }.get().length
}

/* GENERATED_FIR_TAGS: functionDeclaration, functionalType, interfaceDeclaration, lambdaLiteral, nullableType,
stringLiteral, typeParameter, typeWithExtension */
