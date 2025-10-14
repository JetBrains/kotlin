// RUN_PIPELINE_TILL: BACKEND
// WITH_STDLIB
// LANGUAGE: +NameBasedDestructuring, +EnableNameBasedDestructuringShortForm

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
val DestructuringObject.<!EXTENSION_SHADOWED_BY_MEMBER!>eExtProp<!>: String get() = ""

object ObjectSingletonProps {
    const val oConstProp: Int = 1
    @JvmField val oJvmFieldProp: String = ""
    val oValProp: String = ""
    var oVarProp: Int = 2
}

@JvmInline value class ValueClassProps(val vValue: Int)

open class VisibilityCasesBase {
    private val vPrivateProp: Int = 0
    open val vInternalOpenProp: Double = 1.0
    open val vPublicOpenProp: Boolean = true
    val vPublicFinalProp: Int = 1
}
class VisibilityCasesChild : VisibilityCasesBase()

val delegatedCommon by lazy { DestructuringObject(1, "", 3.14) }
val delegatedSingleton by lazy { ObjectSingletonProps }
val delegatedValueClass by lazy { ValueClassProps(1) }
val delegatedVisibilityChild by lazy { VisibilityCasesChild() }

fun destructuringFromDelegatedPositiveShort() {
    val (
        pCProp, pCNullableProp, pCVarProp, pCDefaultProp,
        bProp, bVarProp, bNullableProp,
        cComputedOnly, cComputedVar,
        dLazyProp, dObservableProp, dCustomDelegatedProp,
    ) = delegatedCommon

    val (eExtProp, nOuterProp, aProp, lLateinitVar, aIsActive) = delegatedCommon
    val (oConstProp, oJvmFieldProp) = delegatedSingleton
    val (oVarProp, oValProp) = delegatedSingleton
    val (vValue) = delegatedValueClass
    val (vInternalOpenProp, vPublicOpenProp, vPublicFinalProp) = delegatedVisibilityChild

    check(pCProp + aProp + vValue + vPublicFinalProp + oConstProp >= 0)
    check(aIsActive || vPublicOpenProp)
    bVarProp.length + (bNullableProp?.length ?: 0) + dLazyProp.length + cComputedVar.length + eExtProp.length + nOuterProp.length +
            lLateinitVar.length + oJvmFieldProp.length + oValProp.length
}

fun destructuringFromDelegatedRenamePermuteIgnoreShort() {
    val (renamed = bVarProp, _ = pCProp, also = aProp) = delegatedCommon
    renamed.length + also
}

/* GENERATED_FIR_TAGS: assignment, classDeclaration, const, destructuringDeclaration, functionDeclaration, getter,
integerLiteral, lambdaLiteral, lateinit, localProperty, nullableType, objectDeclaration, operator, primaryConstructor,
propertyDeclaration, propertyDelegate, propertyWithExtensionReceiver, setter, starProjection, stringLiteral, value */
