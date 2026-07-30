// IntelliJ API Decompiler stub source generated from a class file
// Implementation of methods is not available

package kotlin.annotation

public final enum class AnnotationRetention private constructor() : kotlin.Enum<kotlin.annotation.AnnotationRetention> {
    SOURCE,

    BINARY,

    RUNTIME;
}

public final enum class AnnotationTarget private constructor() : kotlin.Enum<kotlin.annotation.AnnotationTarget> {
    CLASS,

    ANNOTATION_CLASS,

    TYPE_PARAMETER,

    PROPERTY,

    FIELD,

    LOCAL_VARIABLE,

    VALUE_PARAMETER,

    CONSTRUCTOR,

    FUNCTION,

    PROPERTY_GETTER,

    PROPERTY_SETTER,

    TYPE,

    EXPRESSION,

    FILE,

    @kotlin.SinceKotlin(version = "1.1")
    TYPEALIAS;
}

@kotlin.annotation.Target(allowedTargets = [kotlin.annotation.AnnotationTarget.ANNOTATION_CLASS])
public final annotation class MustBeDocumented public constructor() : kotlin.Annotation {
}

@kotlin.annotation.Target(allowedTargets = [kotlin.annotation.AnnotationTarget.ANNOTATION_CLASS])
public final annotation class Repeatable public constructor() : kotlin.Annotation {
}

@kotlin.annotation.Target(allowedTargets = [kotlin.annotation.AnnotationTarget.ANNOTATION_CLASS])
public final annotation class Retention public constructor(value: kotlin.annotation.AnnotationRetention = COMPILED_CODE) : kotlin.Annotation {
    public final val value: kotlin.annotation.AnnotationRetention /* compiled code */
}

@kotlin.annotation.Target(allowedTargets = [kotlin.annotation.AnnotationTarget.ANNOTATION_CLASS])
@kotlin.annotation.MustBeDocumented
public final annotation class Target public constructor(vararg allowedTargets: kotlin.annotation.AnnotationTarget) : kotlin.Annotation {
    public final val allowedTargets: kotlin.Array<out kotlin.annotation.AnnotationTarget> /* compiled code */
}
