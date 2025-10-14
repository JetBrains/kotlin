// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE: +NameBasedDestructuring
// WITH_STDLIB

import kotlin.properties.Delegates
import kotlin.reflect.KProperty

class StringDelegate {
    private var value: String = ""
    operator fun getValue(thisRef: Any?, p: KProperty<*>) = value
    operator fun setValue(thisRef: Any?, p: KProperty<*>, v: String) { value = v }
}

open class DestructuringObject(
    val pCProp: Int,
    val pCNullableProp: String?,
    var pCVarProp: Double,
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

private fun DestructuringObject.substring(@Suppress("UNUSED_PARAMETER") i: Int): String = ""

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

private fun ValueClassProps.substring(@Suppress("UNUSED_PARAMETER") i: Int): String = ""

val common = DestructuringObject(1, null, 2.5)
val singleton = ObjectSingletonProps
val valueClass = ValueClassProps(1)
val visibilityChild = VisibilityCasesChild()

fun forDestructuringFromTopLevelPositive() {
    for ((
    val pCProp, val pCNullableProp, val pCVarProp, val pCDefaultProp,
    val bProp, val bVarProp, val bNullableProp,
    val cComputedOnly, val cComputedVar,
    val dLazyProp, val dObservableProp, val dCustomDelegatedProp,
    ) in listOf(common)) { }

    for ((val value, val index) in listOf(common).withIndex()) {
        index.inv()
        value.substring(1)
    }

    for ((val eExtProp, val nOuterProp, val aProp, val lLateinitVar, val aIsActive) in listOf(common)) { }
    for ((val number = aProp, val isActive = aIsActive, val ext = eExtProp) in listOf(common)) { }

    for ((val oConstProp, val oJvmFieldProp) in listOf(singleton)) { }
    for ((val oVarProp, val oValProp) in listOf(singleton)) { }

    for ((val vValue) in listOf(valueClass)) { }

    for ((val vInternalOpenProp, val vPublicOpenProp, val vPublicFinalProp) in listOf(visibilityChild)) { }

    for ((val index, val value) in listOf(valueClass).withIndex()) {
        index.inv()
        value.substring(1)
    }
}

/* GENERATED_FIR_TAGS: assignment, classDeclaration, const, forLoop, funWithExtensionReceiver, functionDeclaration,
getter, integerLiteral, lambdaLiteral, lateinit, localProperty, nullableType, objectDeclaration, operator,
primaryConstructor, propertyDeclaration, propertyDelegate, propertyWithExtensionReceiver, setter, starProjection,
stringLiteral, value */
