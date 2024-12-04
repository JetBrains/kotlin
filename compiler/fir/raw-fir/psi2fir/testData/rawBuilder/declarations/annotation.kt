@Target(
    AnnotationTarget.CLASS,
    AnnotationTarget.PROPERTY,
    AnnotationTarget.LOCAL_VARIABLE,
    AnnotationTarget.VALUE_PARAMETER,
    AnnotationTarget.CONSTRUCTOR,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.TYPE
) annotation class base

@base class correct(@base val x: Int) {
    @base constructor(): this(0)

    @base init {

    }
}

@base enum class My {
    @base FIRST,
    @base SECOND
}

@base fun foo(@base y: @base Int): @base Int {
    @base fun bar(@base z: @base Int) = z + 1
    @base val local = bar(y)
    return local
}

@base val z = 0

@base val x: Map<@base Int, List<@base Int>> = mapOf()
@Target(
    AnnotationTarget.CLASS,
    AnnotationTarget.PROPERTY,
    AnnotationTarget.LOCAL_VARIABLE,
    AnnotationTarget.VALUE_PARAMETER,
    AnnotationTarget.CONSTRUCTOR,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.TYPE
) @base annotation class derived(val x: Int): base

@derived(1) class correctDerived(@derived(1) val x: @derived(1) Int) {
    @base constructor(): this(0)
}

@derived(1) enum class MyDerived {
    @derived(1) FIRST,
    @derived(1) SECOND
}

@derived(1) fun fooDerived(@derived(1) y: @derived(1) Int): @derived(1) Int {
    @derived(1) fun bar(@derived(1) z: @derived(1) Int) = z + 1
    @derived(1) val local = bar(y)
    return local
}

@derived(1) val zDerived = 0

@derived(1) val xDerived: Map<@derived(1) Int, List<@derived(1) Int>> = mapOf()
