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
    open val vInternalOpenProp: Double = 1.0
    open val vPublicOpenProp: Boolean = true
    val vPublicFinalProp: Int = 1
}
class VisibilityCasesChild : VisibilityCasesBase()

val common = DestructuringObject(1, null, 2.5)
val singleton = ObjectSingletonProps
val valueClass = ValueClassProps(1)
val visibilityChild = VisibilityCasesChild()

fun destructuringFromReceiversPositiveShort() {
    common.lLateinitVar = ""

    common.apply {
        val (
            pCProp, pCNullableProp, pCVarProp, pCDefaultProp,
            bProp, bVarProp, bNullableProp,
            cComputedOnly, cComputedVar,
            dLazyProp, dObservableProp, dCustomDelegatedProp
        ) = this
        val (eExtProp, nOuterProp, aProp, aIsActive, lLateinitVar) = this

        bVarProp.length + (bNullableProp?.length ?: 0) + dLazyProp.length +
                cComputedVar.length + nOuterProp.length + lLateinitVar.length +
                pCProp + pCVarProp.toInt() + if (aIsActive) 1 else 0 + bProp +
                dObservableProp + dCustomDelegatedProp.length + cComputedOnly
    }

    common.let {
        val (text = bVarProp, _ = cComputedOnly, num = aProp,) = it
        text.length + num
    }

    common.apply labeled@{
        val (fromLabeled = aProp) = this@labeled
        fromLabeled + 1
    }

    singleton.apply {
        val (oConstProp, oJvmFieldProp, oValProp, oVarProp) = this
        oJvmFieldProp.length + oValProp.length + oConstProp + oVarProp
    }

    valueClass.let { val (vValue) = it; vValue + 1 }

    visibilityChild.apply {
        val (vInternalOpenProp, vPublicOpenProp, vPublicFinalProp) = this
        (if (vPublicOpenProp) 1 else 0) + vPublicFinalProp + vInternalOpenProp.toInt()
    }
}

/* GENERATED_FIR_TAGS: additiveExpression, assignment, classDeclaration, const, destructuringDeclaration,
elvisExpression, functionDeclaration, getter, ifExpression, integerLiteral, lambdaLiteral, lateinit, localProperty,
nullableType, objectDeclaration, operator, primaryConstructor, propertyDeclaration, propertyDelegate,
propertyWithExtensionReceiver, safeCall, setter, starProjection, stringLiteral, thisExpression, unnamedLocalVariable,
value */
