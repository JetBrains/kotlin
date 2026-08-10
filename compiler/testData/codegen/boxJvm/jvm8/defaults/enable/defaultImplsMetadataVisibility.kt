// JVM_DEFAULT_MODE: enable
// TARGET_BACKEND: JVM
// JVM_TARGET: 1.8
// WITH_STDLIB

private const val SYNTHETIC_CLASS_VISIBILITY_SHIFT = 8
private const val SYNTHETIC_CLASS_VISIBILITY_MASK = 0b111
private const val PUBLIC_VISIBILITY = 3
private const val PUBLIC_ABI_FLAG = 1 shl 7

private fun metadataExtraInt(className: String): Int =
    Class.forName(className).getAnnotation(Metadata::class.java).extraInt

private fun syntheticClassVisibility(className: String): Int =
    (metadataExtraInt(className) shr SYNTHETIC_CLASS_VISIBILITY_SHIFT) and SYNTHETIC_CLASS_VISIBILITY_MASK

private fun isPublicAbi(className: String): Boolean =
    metadataExtraInt(className) and PUBLIC_ABI_FLAG != 0

internal interface IInternal {
    fun test(): String = "OK"
}
private interface IPrivate {
    fun test(): String = "OK"
}
interface IPublic {
    fun test(): String = "OK"
}
open class Outer {
    protected interface IProtected {
        fun test(): String = "OK"
    }

    companion object {
        fun getIProtectedClass(): Class<*> = IProtected::class.java
    }
}

fun box(): String {
    val classes = listOf(
        "${IInternal::class.java.name}\$DefaultImpls",
        "${IPrivate::class.java.name}\$DefaultImpls",
        "${Outer.getIProtectedClass().name}\$DefaultImpls",
        "${IPublic::class.java.name}\$DefaultImpls",
    )

    for (className in classes) {
        val visibility = syntheticClassVisibility(className)
        if (visibility != PUBLIC_VISIBILITY) {
            return "Fail [$className]: expected PUBLIC visibility (3), got $visibility"
        }
        // DefaultImpls are public via their visibility, not via the isPublicAbi inline-escape mechanism
        if (isPublicAbi(className)) {
            return "Fail [$className]: expected DefaultImpls to not have isPublicAbi flag set"
        }
    }
    return "OK"
}
