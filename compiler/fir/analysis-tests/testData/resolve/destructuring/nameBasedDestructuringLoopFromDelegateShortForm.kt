// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE: +NameBasedDestructuring, +EnableNameBasedDestructuringShortForm
// WITH_STDLIB

import kotlin.properties.Delegates
import kotlin.reflect.KProperty

class StringDelegate {
    private var v = ""
    operator fun getValue(thisRef: Any?, p: KProperty<*>) = v
    operator fun setValue(thisRef: Any?, p: KProperty<*>, nv: String) { v = nv }
}

open class DestructuringObject(
    val pCProp: Int = 1,
    val pCNullableProp: String? = null,
    var pCVarProp: Double = 0.0,
    val pCDefaultProp: Boolean = true,
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
}
val DestructuringObject.eExtProp: String get() = ""

object ObjectSingletonProps {
    const val oConstProp = 1
    @JvmField val oJvmFieldProp = ""
    val oValProp = ""
    var oVarProp = 2
}
@JvmInline value class ValueClassProps(val vValue: Int)

open class VisibilityCasesBase {
    open val vInternalOpenProp = 1.0
    open val vPublicOpenProp = true
    val vPublicFinalProp = 1
}
class VisibilityCasesChild : VisibilityCasesBase()

val delegatedCommon by lazy { DestructuringObject(1, "", 3.14) }
val delegatedSingleton by lazy { ObjectSingletonProps }
val delegatedValueClass by lazy { ValueClassProps(1) }
val delegatedVisibilityChild by lazy { VisibilityCasesChild() }

fun forLoopsFromDelegatedPositiveShort() {
    for ((
        pCProp, pCNullableProp, pCVarProp, pCDefaultProp,
        bProp, bVarProp, bNullableProp,
        cComputedOnly, cComputedVar,
        dLazyProp, dObservableProp, dCustomDelegatedProp,
    ) in listOf(delegatedCommon)) { }

    for ((eExtProp, nOuterProp, aProp, aIsActive) in listOf(delegatedCommon)) { }

    for ((aIsActive, aProp, nOuterProp,) in listOf(delegatedCommon)) { }

    for ((number = aProp, active = aIsActive, out = nOuterProp) in listOf(delegatedCommon)) { }

    for ((oConstProp, oJvmFieldProp, oValProp, oVarProp) in listOf(delegatedSingleton)) { }

    for ((vValue) in listOf(delegatedValueClass)) { }

    for ((vInternalOpenProp, vPublicOpenProp, vPublicFinalProp) in listOf(delegatedVisibilityChild)) { }
}

/* GENERATED_FIR_TAGS: assignment, classDeclaration, const, forLoop, functionDeclaration, getter, integerLiteral,
lambdaLiteral, lateinit, localProperty, nullableType, objectDeclaration, operator, primaryConstructor,
propertyDeclaration, propertyDelegate, propertyWithExtensionReceiver, setter, starProjection, stringLiteral, value */
