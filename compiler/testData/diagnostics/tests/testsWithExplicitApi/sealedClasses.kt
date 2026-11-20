// RUN_PIPELINE_TILL: CODEGEN

public sealed class Sealed protected constructor(public val y: Int) {
    private constructor() : this(42)
    protected constructor(x: Double) : this(x.toInt())
    constructor(z: Float) : this(z.toInt())

    public object Impl : Sealed(0)
}

public sealed class SealedWithDefaultPrimary {
    constructor(z: Float)
    protected constructor(x: Int)
}

internal sealed class InternalSealed {
    constructor(z: Float)
    protected constructor(x: Int)
}

/* GENERATED_FIR_TAGS: classDeclaration, integerLiteral, nestedClass, objectDeclaration, primaryConstructor,
propertyDeclaration, sealed, secondaryConstructor */
