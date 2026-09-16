// RUN_PIPELINE_TILL: CODEGEN
interface Foo

class Bar(f: Foo) : Foo by f {
    // Backing field is renamed to `$$delegate_0$1`
    val `$$delegate_0`: Foo? = null
}

class Bar2(f: Foo) :
    // Backing field for delegate is renamed to `$$delegate_0$1`
    Foo by f {

    lateinit var `$$delegate_0`: Foo
}

/* GENERATED_FIR_TAGS: classDeclaration, inheritanceDelegation, interfaceDeclaration, lateinit, nullableType,
primaryConstructor, propertyDeclaration */
