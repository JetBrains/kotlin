// IGNORE_BACKEND: JVM
// WITH_REFLECT
// LANGUAGE: +JvmPermittedSubclassesAttributeForSealed

sealed class Base
class O : Base()
class K : Base()

sealed interface IBase
class X : IBase
class Y : IBase

fun box(): String {
    val cBase = Base::class.java
    if (!cBase.isSealed) return "Error: Base is not sealed"
    val pBase = cBase.permittedSubclasses.mapTo(HashSet()) { it.simpleName ?: "???" }
    if (pBase != setOf("O", "K")) {
        return "Failed: $pBase"
    }

    val cIBase = IBase::class.java
    if (!cIBase.isSealed) return "Error: IBase is not sealed"
    val pIBase = cIBase.permittedSubclasses.mapTo(HashSet()) { it.simpleName ?: "???" }
    if (pIBase != setOf("X", "Y")) {
        return "Failed: $pIBase"
    }

    return "OK"
}
