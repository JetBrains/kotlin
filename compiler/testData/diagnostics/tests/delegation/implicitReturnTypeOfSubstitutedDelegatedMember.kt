// RUN_PIPELINE_TILL: CODEGEN
// ISSUE: KT-89635
// The implicitly typed callers come first so that they trigger the implicit type resolution of the delegated members

fun <B> useFunction(w: Wrap<B>) = w.id(w.produce())
fun <B> useProperty(w: Wrap<B>) = w.prop

fun checkFunction(w: Wrap<String>): String = useFunction(w)
fun checkProperty(w: Wrap<String>): String = useProperty(w)

abstract class Wrap<A>(delegate: Intf<A>) : Intf<A> by delegate {
    abstract fun produce(): A
}

interface Intf<X> {
    fun id(x: X) = x
    fun get(): X
    val prop get() = get()
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, getter, inheritanceDelegation, interfaceDeclaration,
nullableType, primaryConstructor, propertyDeclaration, typeParameter */
