// RUN_PIPELINE_TILL: BACKEND
abstract class A {
    open val <D> Inv<D>.phasedFir: D get() = TODO()
}

abstract class B : A() {
    final override val <D> Inv<D>.phasedFir: D  get() = TODO()
}

abstract class Inv<E>

class C : B() {
    fun foo(x: Inv<String>) {
        x.phasedFir
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, getter, nullableType, override, propertyDeclaration,
propertyWithExtensionReceiver, typeParameter */
