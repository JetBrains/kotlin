// MODULE: lib
// FILE: A.kt
open class A {
    private fun onlyInternal() = "onlyInternal"
    private fun internalAndPublic() = "internalAndPublic"
    private fun internalAndProtected() = "internalAndProtected"
    private fun internalAndInternalPA() = "internalAndInternalPA"
    private fun onlyPublic() = "onlyPublic"
    private fun onlyProtected() = "onlyProtected"
    private fun onlyInternalPA() = "onlyInternalPA"
    private fun allEffectivelyPublic() = "allEffectivelyPublic"

    internal inline fun inlineOnlyInternal1() = onlyInternal()
    internal inline fun inlineOnlyInternal2() = onlyInternal()

    internal inline fun inlineInternalAndPublic1() = internalAndPublic()

    @Suppress("NON_PUBLIC_CALL_FROM_PUBLIC_INLINE")
    public inline fun inlineInternalAndPublic2() = internalAndPublic()

    internal inline fun inlineInternalAndProtected1() = internalAndProtected()

    @Suppress("NON_PUBLIC_CALL_FROM_PUBLIC_INLINE")
    protected inline fun inlineInternalAndProtected2() = internalAndProtected()

    internal inline fun inlineInternalAndInternalPA1() = internalAndInternalPA()

    @Suppress("NON_PUBLIC_CALL_FROM_PUBLIC_INLINE")
    @PublishedApi
    internal inline fun inlineInternalAndInternalPA2() = internalAndInternalPA()

    @Suppress("NON_PUBLIC_CALL_FROM_PUBLIC_INLINE")
    public inline fun inlineOnlyPublic1() = onlyPublic()

    @Suppress("NON_PUBLIC_CALL_FROM_PUBLIC_INLINE")
    public inline fun inlineOnlyPublic2() = onlyPublic()

    @Suppress("NON_PUBLIC_CALL_FROM_PUBLIC_INLINE")
    protected inline fun inlineOnlyProtected1() = onlyProtected()

    @Suppress("NON_PUBLIC_CALL_FROM_PUBLIC_INLINE")
    protected inline fun inlineOnlyProtected2() = onlyProtected()

    @Suppress("NON_PUBLIC_CALL_FROM_PUBLIC_INLINE")
    @PublishedApi
    internal inline fun inlineOnlyInternalPA1() = onlyInternalPA()

    @Suppress("NON_PUBLIC_CALL_FROM_PUBLIC_INLINE")
    @PublishedApi
    internal inline fun inlineOnlyInternalPA2() = onlyInternalPA()

    @Suppress("NON_PUBLIC_CALL_FROM_PUBLIC_INLINE")
    public inline fun inlineAllEffectivelyPublic1() = allEffectivelyPublic()

    @Suppress("NON_PUBLIC_CALL_FROM_PUBLIC_INLINE")
    protected inline fun inlineAllEffectivelyPublic2() = allEffectivelyPublic()

    @Suppress("NON_PUBLIC_CALL_FROM_PUBLIC_INLINE")
    @PublishedApi
    internal inline fun inlineAllEffectivelyPublic3() = allEffectivelyPublic()
}

// MODULE: main(lib)
// FILE: main.kt
fun box(): String {
    A() // access class A to load all its members
    return "OK"
}
