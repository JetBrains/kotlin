// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-43936
// WITH_STDLIB

import FooOperation.*

interface Operation<T>

class FooOperation(val foo: String) : Operation<Boom> {

    @Suppress("test")
    class Boom(val bar: String)
}

/* GENERATED_FIR_TAGS: classDeclaration, interfaceDeclaration, nestedClass, nullableType, primaryConstructor,
propertyDeclaration, stringLiteral, typeParameter */
