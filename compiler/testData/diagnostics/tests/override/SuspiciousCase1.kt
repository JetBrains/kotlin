// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
// NamedFunctionDescriptor.substitute substitutes "overrides"
// this test checks it does it properly

interface Foo<P> {
    fun quux(p: P, q: Int = 17) : Int = 18
}

interface Bar<Q> : Foo<Q>

abstract class Baz() : Bar<String>

fun zz(b: Baz) = b.quux("a")

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, integerLiteral, interfaceDeclaration, nullableType,
primaryConstructor, stringLiteral, typeParameter */
