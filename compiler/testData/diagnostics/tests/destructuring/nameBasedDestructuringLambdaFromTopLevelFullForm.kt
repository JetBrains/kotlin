// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE: +NameBasedDestructuring
// WITH_STDLIB

import kotlin.properties.Delegates
import kotlin.reflect.KProperty

class StringDelegate {
    private var v = ""
    operator fun getValue(thisRef: Any?, p: KProperty<*>) = v
    operator fun setValue(thisRef: Any?, p: KProperty<*>, nv: String) { v = nv }
}

open class DestructuringObject(
    val pCProp: Int,
    val pCNullableProp: String?,
    var pCVarProp: Double,
    val pCDefaultProp: Boolean = true
) {
    val bProp = 1
    var bVarProp = ""
    val bNullableProp: String? = null

    val cComputedOnly get() = 1
    var cComputedVar: String
        get() = ""
        set(@Suppress("UNUSED_PARAMETER") v) {}

    val dLazyProp by lazy { "" }
    var dObservableProp: Int by Delegates.observable(1) { _,_,_ -> }
    var dCustomDelegatedProp: String by StringDelegate()

    val nOuterProp = ""
    val aProp = 1
    val aIsActive = true
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

val common = DestructuringObject(1, null, 2.5)
val singleton = ObjectSingletonProps
val valueClass = ValueClassProps(1)
val visibilityChild = VisibilityCasesChild()

inline fun consumeCommon(f: (DestructuringObject) -> Unit) = f(common)
inline fun consumeSingleton(f: (ObjectSingletonProps) -> Unit) = f(singleton)
inline fun consumeValue(f: (ValueClassProps) -> Unit) = f(valueClass)
inline fun consumeVisibility(f: (VisibilityCasesChild) -> Unit) = f(visibilityChild)
inline fun consumeTwo(f: (DestructuringObject, ObjectSingletonProps) -> Unit) = f(common, singleton)

fun lambdaParamsFromTopLevelPositiveFull() {
    consumeCommon { (
        val pCProp, val pCNullableProp, val pCVarProp, val pCDefaultProp,
        val bProp, val bVarProp, val bNullableProp,
        val cComputedOnly, val cComputedVar,
        val dLazyProp, val dObservableProp, val dCustomDelegatedProp,
        ) -> Unit }

    consumeCommon { (val eExtProp, val nOuterProp, val aProp, val lLateinitVar, val aIsActive) -> Unit }
    consumeCommon { (val number = aProp, val isActive = aIsActive, val ext = eExtProp) -> Unit }

    consumeSingleton { (val oConstProp, val oJvmFieldProp) -> Unit }
    consumeSingleton { (val oVarProp, val oValProp) -> Unit }

    consumeValue { (val vValue) -> Unit }
    consumeVisibility { (val vInternalOpenProp, val vPublicOpenProp, val vPublicFinalProp) -> Unit }

    consumeTwo { (val a = aProp), (val c = oConstProp) -> Unit }
}

/* GENERATED_FIR_TAGS: assignment, classDeclaration, const, functionDeclaration, functionalType, getter, inline,
integerLiteral, lambdaLiteral, lateinit, localProperty, nullableType, objectDeclaration, operator, primaryConstructor,
propertyDeclaration, propertyDelegate, propertyWithExtensionReceiver, setter, starProjection, stringLiteral, value */
