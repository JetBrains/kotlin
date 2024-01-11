// FIR_IDENTICAL
@file:OptIn(ExperimentalSubclassOptIn::class)

@RequiresOptIn
annotation class Boom

@RequiresOptIn
annotation class Boom2

@SubclassOptInRequired(Boom::class)
open class B {
    @SubclassOptInRequired(Boom2::class)
    open inner class C
}


fun test() {
    with(B()) {
        @OptIn(Boom2::class)
        class Local : B.C() {}
    }
}

