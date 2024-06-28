// FIR_IDENTICAL
@file:OptIn(ExperimentalSubclassOptIn::class)

@RequiresOptIn
annotation class Boom

@RequiresOptIn
annotation class Boom2

@SubclassOptInRequired(Boom::class)
open class B {
    @SubclassOptInRequired(Boom2::class)
    open class C

    @OptIn(Boom2::class)
    class C2 : C()
}

@OptIn(Boom2::class)
class E2 : B.C() {}
