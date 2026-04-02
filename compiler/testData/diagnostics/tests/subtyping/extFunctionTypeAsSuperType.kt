// RUN_PIPELINE_TILL: FRONTEND
class A : <!SUPERTYPE_IS_EXTENSION_OR_CONTEXT_FUNCTION_TYPE!>Double.() -> Unit<!> {
    override fun invoke(p1: Double) {}
}

class B : <!SUPERTYPE_IS_EXTENSION_OR_CONTEXT_FUNCTION_TYPE!>Int.(Double) -> Unit<!> {
    override fun invoke(p1: Int, p2: Double) {}
}

open class C {}

abstract class A0 : <!SUPERTYPE_IS_EXTENSION_OR_CONTEXT_FUNCTION_TYPE!>C.() -> Int<!>
abstract class A1 : <!SUPERTYPE_IS_EXTENSION_OR_CONTEXT_FUNCTION_TYPE!>C.(Int) -> Int<!>
abstract class A2 : <!SUPERTYPE_IS_EXTENSION_OR_CONTEXT_FUNCTION_TYPE!>C.(Int, String) -> Int<!>

typealias Foo = C.() -> Int

abstract class A4 : <!SUPERTYPE_IS_EXTENSION_OR_CONTEXT_FUNCTION_TYPE!>Foo<!>

open class D<T> {}

abstract class B0<T> : <!SUPERTYPE_IS_EXTENSION_OR_CONTEXT_FUNCTION_TYPE!>D<T>.() -> Int<!>
abstract class B1<T> : <!SUPERTYPE_IS_EXTENSION_OR_CONTEXT_FUNCTION_TYPE!>D<T>.(C) -> Int<!>
abstract class B2<T> : <!SUPERTYPE_IS_EXTENSION_OR_CONTEXT_FUNCTION_TYPE!>D<T>.(T, C) -> T<!>

interface E<T> {}

abstract class C0: C(), <!SUPERTYPE_IS_EXTENSION_OR_CONTEXT_FUNCTION_TYPE!>Int.() -> Double<!>
abstract class C1<T>: C(), E<T>, <!SUPERTYPE_IS_EXTENSION_OR_CONTEXT_FUNCTION_TYPE!>Int.(C) -> Double<!>

abstract class D0 : <!SUPERTYPE_IS_EXTENSION_OR_CONTEXT_FUNCTION_TYPE!>context(C) () -> Int<!>

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, functionalType, interfaceDeclaration, nullableType,
operator, override, typeAliasDeclaration, typeParameter, typeWithContext, typeWithExtension */
