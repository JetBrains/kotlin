// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +CollectionLiterals

class C {
    companion object {
        operator fun of(vararg xs: Int): C = C()
    }
}

interface Setter<U> {
    fun set(u: U)
}

fun test(
    d: Setter<in C>,
    e: Setter<out C>,
    f: Setter<*>,
) {
    d.set([])
    e.set(<!UNRESOLVED_REFERENCE!>[]<!>)
    f.set(<!UNRESOLVED_REFERENCE!>[]<!>)

    val x = select(materializeIn<C>(), C.of()) // for reference
    val y = select(materializeIn<C>(), [])

    selectDnn(materializeIn<C?>(), C.of()) // for reference
    selectDnn(materializeIn<C?>(), [])
}

fun <D> materializeIn(): Setter<in D> = null!!
fun <E> select(x: Setter<E>, y: E) = y
fun <F> selectDnn(x: Setter<F>, y: F & Any) = y

/* GENERATED_FIR_TAGS: capturedType, classDeclaration, collectionLiteral, companionObject, functionDeclaration,
inProjection, interfaceDeclaration, nullableType, objectDeclaration, operator, outProjection, starProjection,
typeParameter, vararg */
