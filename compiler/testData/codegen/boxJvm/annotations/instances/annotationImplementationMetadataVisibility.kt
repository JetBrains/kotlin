// TARGET_BACKEND: JVM
// WITH_STDLIB

private const val SYNTHETIC_CLASS_VISIBILITY_SHIFT = 8
private const val SYNTHETIC_CLASS_VISIBILITY_MASK = 0b111
private const val LOCAL_VISIBILITY = 5
private const val PUBLIC_ABI_FLAG = 1 shl 7

private fun syntheticClassVisibility(javaClass: Class<*>): Int {
    val extraInt = javaClass.getAnnotation(Metadata::class.java).extraInt
    return (extraInt shr SYNTHETIC_CLASS_VISIBILITY_SHIFT) and SYNTHETIC_CLASS_VISIBILITY_MASK
}
private fun metadataExtraInt(javaClass: Class<*>): Int =
    javaClass.getAnnotation(Metadata::class.java).extraInt

private fun isPublicAbi(javaClass: Class<*>): Boolean =
    metadataExtraInt(javaClass) and PUBLIC_ABI_FLAG != 0

annotation class Ann(val value: String)
annotation class InlineAnn(val value: String)
annotation class PrivateInlineAnn(val value: String)

inline fun createAnn(): Class<*> = InlineAnn("OK").javaClass

private inline fun privateCreateAnn(): Class<*> = PrivateInlineAnn("OK").javaClass

fun box(): String {
    val ann = Ann("OK")
    val inlineAnn = createAnn()
    val privateInlineAnn = privateCreateAnn()

    var visibility = syntheticClassVisibility(ann.javaClass)
    if (visibility != LOCAL_VISIBILITY) {
        return "Fail: expected LOCAL visibility (5), got $visibility"
    }

    if (isPublicAbi(ann.javaClass)) {
        return "Fail: expected annotation implementation class to NOT be public ABI in non-inline context"
    }

    visibility = syntheticClassVisibility(createAnn())
    if (visibility != LOCAL_VISIBILITY) {
        return "Fail: expected LOCAL visibility (5), got $visibility"
    }

    if (!isPublicAbi(createAnn())) {
        return "Fail: expected annotation implementation class to be public ABI in inline context"
    }

    visibility = syntheticClassVisibility(privateCreateAnn())
    if (visibility != LOCAL_VISIBILITY) {
        return "Fail: expected LOCAL visibility (5), got $visibility"
    }

    if (isPublicAbi(privateCreateAnn())) {
        return "Fail: expected annotation implementation class to NOT be public ABI in private inline context"
    }

    return ann.value
}
