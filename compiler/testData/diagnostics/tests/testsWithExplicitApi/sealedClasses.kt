// RUN_PIPELINE_TILL: FRONTEND

public sealed class Sealed protected constructor(public val y: Int) {
    private constructor() : this(42)
    protected constructor(x: Double) : this(x.toInt())
    <!NO_EXPLICIT_VISIBILITY_IN_API_MODE!>constructor<!>(z: Float) : this(z.toInt())

    public object Impl : Sealed(0)
}

public sealed class SealedWithDefaultPrimary {
    <!NO_EXPLICIT_VISIBILITY_IN_API_MODE!>constructor<!>(z: Float)
    protected constructor(x: Int)
}

internal sealed class InternalSealed {
    constructor(z: Float)
    protected constructor(x: Int)
}

/* GENERATED_FIR_TAGS: classDeclaration, integerLiteral, nestedClass, objectDeclaration, primaryConstructor,
propertyDeclaration, sealed, secondaryConstructor */
