// MUTE_SIGNATURE_COMPARISON_K2: ANY
// ^ KT-57754

interface IFoo {
    @Deprecated("")
    val prop: String get() = ""

    @Deprecated("")
    val String.extProp: String get() = ""
}

class Delegated(foo: IFoo) : IFoo by foo

class DefaultImpl : IFoo

class ExplicitOverride : IFoo {
    override val prop: String get() = ""
    override val String.extProp: String get() = ""
}
