// RUN_PIPELINE_TILL: BACKEND
// WITH_STDLIB
// LANGUAGE: +NameBasedDestructuring

import kotlin.properties.Delegates
import kotlin.reflect.KProperty

class StringDelegate {
    private var value: String = ""
    operator fun getValue(thisRef: Any?, property: KProperty<*>) = value
    operator fun setValue(thisRef: Any?, property: KProperty<*>, v: String) { value = v }
}

open class DestructuringObject(
    val pCProp: Int,
    val pCNullableProp: String?,
    var pCVarProp: Double,
    val pCDefaultProp: Boolean = true
) {
    val bProp: Int = 1
    var bVarProp: String = ""
    val bNullableProp: String? = null

    val cComputedOnly: Int get() = 1
    var cComputedVar: String
        get() = ""
        set(@Suppress("UNUSED_PARAMETER") v) {}

    val dLazyProp: String by lazy { "" }
    var dObservableProp: Int by Delegates.observable(1) { _, _, _ -> }
    var dCustomDelegatedProp: String by StringDelegate()

    val nOuterProp: String = ""
    val aProp: Int = 1
    val aIsActive: Boolean = true
    lateinit var lLateinitVar: String

    val eExtProp: String = "member"
}
val DestructuringObject.<!EXTENSION_SHADOWED_BY_MEMBER!>eExtProp<!>: String get() = "extension"

object ObjectSingletonProps {
    const val oConstProp: Int = 1
    @JvmField val oJvmFieldProp: String = ""
    val oValProp: String = ""
    var oVarProp: Int = 2
}

@JvmInline value class ValueClassProps(val vValue: Int)

open class VisibilityCasesBase {
    open val vInternalOpenProp: Double = 1.0
    open val vPublicOpenProp: Boolean = true
    val vPublicFinalProp: Int = 1
}
open class VisibilityCasesChild : VisibilityCasesBase() {
    override val vInternalOpenProp: Double = 1.0
    override val vPublicOpenProp: Boolean = true
}

val common = DestructuringObject(1, null, 2.5)
val singleton = ObjectSingletonProps
val valueClass = ValueClassProps(1)
val visibilityChild = VisibilityCasesChild()

fun destructuringFromTopLevel_positive() {
    common.lLateinitVar = ""

    (
    val pCProp, val pCNullableProp, val pCVarProp, val pCDefaultProp,
    val bProp, val bVarProp, val bNullableProp,
    val cComputedOnly, val cComputedVar,
    val dLazyProp, val dObservableProp, val dCustomDelegatedProp
    ) = common

    (val picked = eExtProp) = common
    picked.length + bVarProp.length + (bNullableProp?.length ?: 0) + dLazyProp.length + cComputedVar.length

    (val number = aProp, val _ = pCNullableProp, val isActive = aIsActive,) = common
    number + if (isActive) 1 else 0

    (val oConstProp, val oJvmFieldProp) = singleton
    (val oVarProp, val oValProp) = singleton
    oConstProp + oVarProp + oJvmFieldProp.length + oValProp.length

    (val vValue) = valueClass
    vValue + 1

    (val vInternalOpenProp, val vPublicOpenProp, val vPublicFinalProp) = visibilityChild
    vInternalOpenProp.toInt() + vPublicFinalProp + if (vPublicOpenProp) 1 else 0
}

fun destructuringFromTopLevelVarLocalsOnly() {
    (var n = aProp, var active = aIsActive) = common
    n = n + (if (active) 1 else 0)
}

/* GENERATED_FIR_TAGS: additiveExpression, assignment, classDeclaration, const, destructuringDeclaration,
elvisExpression, functionDeclaration, getter, ifExpression, integerLiteral, lambdaLiteral, lateinit, localProperty,
nullableType, objectDeclaration, operator, override, primaryConstructor, propertyDeclaration, propertyDelegate,
propertyWithExtensionReceiver, safeCall, setter, starProjection, stringLiteral, unnamedLocalVariable, value */
