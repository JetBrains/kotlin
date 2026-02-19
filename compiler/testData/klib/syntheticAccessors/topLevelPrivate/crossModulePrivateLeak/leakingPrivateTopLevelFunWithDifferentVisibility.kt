// MODULE: lib
// FILE: A.kt
private fun onlyInternal() = "onlyInternal"
private fun internalAndPublic() = "internalAndPublic"
private fun internalAndInternalPA() = "internalAndInternalPA"
private fun onlyPublic() = "onlyPublic"
private fun onlyInternalPA() = "onlyInternalPA"
private fun allEffectivelyPublic() = "allEffectivelyPublic"

internal inline fun inlineOnlyInternal1() = onlyInternal()
internal inline fun inlineOnlyInternal2() = onlyInternal()

internal inline fun inlineInternalAndPublic1() = internalAndPublic()
@Suppress("NON_PUBLIC_CALL_FROM_PUBLIC_INLINE")
public inline fun inlineInternalAndPublic2() = internalAndPublic()

internal inline fun inlineInternalAndInternalPA1() = internalAndInternalPA()
@Suppress("NON_PUBLIC_CALL_FROM_PUBLIC_INLINE")
@PublishedApi
internal inline fun inlineInternalAndInternalPA2() = internalAndInternalPA()

@Suppress("NON_PUBLIC_CALL_FROM_PUBLIC_INLINE")
public inline fun inlineOnlyPublic1() = onlyPublic()
@Suppress("NON_PUBLIC_CALL_FROM_PUBLIC_INLINE")
public inline fun inlineOnlyPublic2() = onlyPublic()

@Suppress("NON_PUBLIC_CALL_FROM_PUBLIC_INLINE")
@PublishedApi
internal inline fun inlineOnlyInternalPA1() = onlyInternalPA()
@Suppress("NON_PUBLIC_CALL_FROM_PUBLIC_INLINE")
@PublishedApi
internal inline fun inlineOnlyInternalPA2() = onlyInternalPA()

@Suppress("NON_PUBLIC_CALL_FROM_PUBLIC_INLINE")
public inline fun inlineAllEffectivelyPublic1() = allEffectivelyPublic()
@Suppress("NON_PUBLIC_CALL_FROM_PUBLIC_INLINE")
@PublishedApi
internal inline fun inlineAllEffectivelyPublic2() = allEffectivelyPublic()

// MODULE: main()(lib)
// FILE: main.kt
fun box(): String {
    // call top-level functions from another module to load them all:
    inlineOnlyInternal1()
    inlineOnlyInternal2()
    inlineInternalAndPublic1()
    inlineInternalAndPublic2()
    inlineInternalAndInternalPA1()
    inlineInternalAndInternalPA2()
    inlineOnlyPublic1()
    inlineOnlyPublic2()
    inlineOnlyInternalPA1()
    inlineOnlyInternalPA2()
    inlineAllEffectivelyPublic1()
    inlineAllEffectivelyPublic2()

    return "OK"
}
