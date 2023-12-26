// TARGET_BACKEND: JVM_IR
// WITH_STDLIB
// JVM_ABI_K1_K2_DIFF: KT-63828

interface PsiOwner {
    var psi: String?
}

class PsiOwnerImpl(override var psi: String? = null) : PsiOwner

interface JKElement

interface JKFormattingOwner

abstract class JKTreeElement : JKElement, JKFormattingOwner

abstract class JKDeclaration : JKTreeElement(), PsiOwner by PsiOwnerImpl()

interface JKAnnotationListOwner : JKFormattingOwner

open class JKVariable : JKDeclaration(), JKAnnotationListOwner

class JKEnumConstant : JKVariable()

fun box(): String {
    val constant = JKEnumConstant().also { it.psi = "OK" }
    return constant.psi!!
}
