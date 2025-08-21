// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
// FIR_DUMP
// ISSUE: KT-68159

class Foo
class GenericContainer<G>

typealias FooAlias = Foo

class Test {
    // All should resolve to `{FooAlias=} Foo`
    lateinit var lib: FooAlias
    val containerCall = GenericContainer<FooAlias>()
    lateinit var containerType: GenericContainer<FooAlias>
}

/* GENERATED_FIR_TAGS: classDeclaration, lateinit, nullableType, propertyDeclaration, typeAliasDeclaration,
typeParameter */
