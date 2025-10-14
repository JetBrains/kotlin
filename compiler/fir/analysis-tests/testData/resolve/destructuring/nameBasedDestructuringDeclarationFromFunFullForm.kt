// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE: +NameBasedDestructuring
// WITH_STDLIB

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
    val eExtProp: String = "member"
}

val DestructuringObject.<!EXTENSION_SHADOWED_BY_MEMBER!>eExtProp<!>: String get() = "extension"

object ObjectSingletonProps {
    @JvmField val oJvmFieldProp: String = ""
    val oValProp: String = ""
}
@JvmInline value class ValueClassProps(val vValue: Int)

open class VisibilityCasesBase {
    open val vPublicOpenProp: Boolean = true
    val vPublicFinalProp: Int = 1
}
class VisibilityCasesChild : VisibilityCasesBase()

val common = DestructuringObject(1, null, 2.5)
val singleton = ObjectSingletonProps
val valueClass = ValueClassProps(1)
val visibilityChild = VisibilityCasesChild()

@JvmName("getCommonFun") fun getCommon() = common
@JvmName("getSingletonFun") fun getSingleton() = singleton
@JvmName("getValueClassFun") fun getValueClass() = valueClass
@JvmName("getVisibilityChildFun") fun getVisibilityChild() = visibilityChild

fun <T> id(x: T): T = x

val delegatedCommon by lazy { DestructuringObject(1, "", 3.14) }

fun destructuringFromFunctionsBase() {
    (
    val pCProp, val pCNullableProp, val pCVarProp, val pCDefaultProp,
    val bProp, val bVarProp, val bNullableProp,
    val cComputedOnly, val cComputedVar,
    val dLazyProp, val dObservableProp, val dCustomDelegatedProp,
    val nOuterProp,
    val aProp, val aIsActive
    ) = getCommon()

    (val isActive = aIsActive, val _ = pCVarProp, val num = aProp,) = getCommon()
    (val picked = eExtProp) = getCommon()

    (val oValProp, val oJvmFieldProp) = getSingleton()
    (val vValue) = getValueClass()
    (val vPublicOpenProp, val vPublicFinalProp) = getVisibilityChild()

    picked.length + bVarProp.length + (bNullableProp?.length ?: 0) + dLazyProp.length + cComputedVar.length + nOuterProp.length + num
}

fun destructuringFromFunctions() {
    (val oValProp, val oJvmFieldProp) = getSingleton()
    (val vValue) = getValueClass()
    (val vPublicOpenProp, val vPublicFinalProp) = getVisibilityChild()
    (val again = aProp) = id(getCommon())

    oJvmFieldProp.length + oValProp.length + vValue + vPublicFinalProp + if (vPublicOpenProp) 1 else 0 + again
}

/* GENERATED_FIR_TAGS: assignment, classDeclaration, destructuringDeclaration, functionDeclaration, getter,
integerLiteral, lambdaLiteral, localProperty, nullableType, objectDeclaration, operator, override, primaryConstructor,
propertyDeclaration, propertyDelegate, setter, starProjection, stringLiteral, value */
