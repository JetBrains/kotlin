// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE: +NameBasedDestructuring, +EnableNameBasedDestructuringShortForm
// WITH_STDLIB
package test.destructuring.objects

import kotlin.properties.Delegates
import kotlin.reflect.KProperty

class StringDelegate {
    private var value: String = ""
    operator fun getValue(thisRef: Any?, property: KProperty<*>) = value
    operator fun setValue(thisRef: Any?, property: KProperty<*>, newValue: String) { value = newValue }
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
    const val oConstProp: Int = 1
    @JvmField val oJvmFieldProp: String = ""
    val oValProp: String = ""
    var oVarProp: Int = 2
}
@JvmInline value class ValueClassProps(val vValue: Int)
open class VisibilityCasesBase { open val vInternalOpenProp: Double = 1.0; open val vPublicOpenProp: Boolean = true; val vPublicFinalProp: Int = 1 }
class VisibilityCasesChild : VisibilityCasesBase()

val common = DestructuringObject(pCProp = 1, pCNullableProp = "s", pCVarProp = 2.5)
val singleton = ObjectSingletonProps
val valueClass = ValueClassProps(1)
val visibilityChild = VisibilityCasesChild()

fun lambdaParamsFromReceiversPositiveShort() {
    common.let {
            (
                pCProp, pCNullableProp, pCVarProp, pCDefaultProp,
                bProp, bVarProp, bNullableProp,
                cComputedOnly, cComputedVar,
                dLazyProp, dObservableProp, dCustomDelegatedProp,
            ) -> Unit
    }

    common.let { (eExtProp, nOuterProp, aProp, aIsActive) -> Unit }

    common.let {
        (
                aIsActive,
        number = aProp,
        pCProp,
        pCVarProp,
        ) -> Unit
    }

    singleton.let { (oConstProp, oJvmFieldProp, oValProp, oVarProp) -> Unit }
    valueClass.let { (vValue) -> Unit }
    visibilityChild.let { (vInternalOpenProp, vPublicOpenProp, vPublicFinalProp) -> Unit }
}

/* GENERATED_FIR_TAGS: assignment, classDeclaration, const, functionDeclaration, getter, integerLiteral, lambdaLiteral,
lateinit, localProperty, nullableType, objectDeclaration, operator, primaryConstructor, propertyDeclaration,
propertyDelegate, propertyWithExtensionReceiver, setter, starProjection, stringLiteral, value */
