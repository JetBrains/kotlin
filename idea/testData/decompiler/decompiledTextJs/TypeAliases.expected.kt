// IntelliJ API Decompiler stub source generated from a class file
// Implementation of methods is not available

package test

@kotlin.annotation.Target public final annotation class Ann public constructor(value: kotlin.String) : kotlin.Annotation {
    public final val value: kotlin.String /* compiled code */
}

public final class Outer<E, F> public constructor() {
    public final inner class Inner<G> public constructor() {
        @kotlin.Suppress public typealias TA<H>  = kotlin.collections.Map<kotlin.collections.Map<E, F>, kotlin.collections.Map<G, H>>
    }
}

public final class TypeAliases public constructor() {
    public final fun foo(a: dependency.A /* = () -> kotlin.Unit */, b: test.TypeAliases.B /* = (dependency.A /* = () -> kotlin.Unit */) -> kotlin.Unit */, ta: test.Outer<kotlin.String, kotlin.Double>.Inner<kotlin.Int>.TA<kotlin.Boolean> /* = kotlin.collections.Map<kotlin.collections.Map<kotlin.String, kotlin.Double>, kotlin.collections.Map<kotlin.Int, kotlin.Boolean>> */): kotlin.Unit { /* compiled code */ }

    @kotlin.Suppress public typealias B = (dependency.A) -> kotlin.Unit

    @test.Ann @kotlin.Suppress private typealias Parametrized<E, F>  = kotlin.collections.Map<E, F>
}

