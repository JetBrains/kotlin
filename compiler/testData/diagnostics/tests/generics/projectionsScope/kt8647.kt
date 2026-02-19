// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
interface Subscriber<T>

interface Observable<T> {
    fun subscribe(s: Subscriber<in T>)
}

fun foo(o: Observable<out CharSequence>, y: Subscriber<in CharSequence>) = o.subscribe(y) // type safe

/* GENERATED_FIR_TAGS: functionDeclaration, inProjection, interfaceDeclaration, nullableType, outProjection,
typeParameter */
