// RUN_PIPELINE_TILL: BACKEND
// WITH_STDLIB
// LANGUAGE: +NameBasedDestructuring

import kotlin.properties.Delegates
import kotlin.reflect.KProperty

typealias NBAliasInt = Int
typealias NBAliasString = String
typealias NBAliasNullableString = String?
typealias NBAliasNumber = Number

@Target(AnnotationTarget.PROPERTY, AnnotationTarget.FIELD, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class DSName(vararg val value: String)

class StringDelegate {
    private var value: String = ""
    operator fun getValue(thisRef: Any?, property: KProperty<*>): String = value
    operator fun setValue(thisRef: Any?, property: KProperty<*>, newValue: String) { value = newValue }
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
        set(@Suppress("UNUSED_PARAMETER") value) {}

    val dLazyProp: String by lazy { "" }
    var dObservableProp: Int by Delegates.observable(1) { _, _, _ -> }
    var dCustomDelegatedProp: String by StringDelegate()

    @get:JvmName("getRenamedNum")
    @DSName("renamedNum", "number")
    val aProp: Int = 1

    @get:JvmName("isActiveJvm")
    @DSName("active", "isActive")
    val aIsActive: Boolean = true

    val nOuterProp: String = ""
}
val DestructuringObject.eExtProp: String get() = ""

class GenericDestructuringObject<T, in I, out O>(
    val pCDefaultProp: Boolean = true,
    val pCPropOutO: O,
    val pCInConsumer: (I) -> Int
) {
    val bProp: Int = 1
    var bVarProp: String = ""
    val bNullableProp: String? = null

    val cComputedOnly: Int get() = 1
    var cComputedVar: String
        get() = ""
        set(@Suppress("UNUSED_PARAMETER") value) {}

    val dLazyProp: String by lazy { "" }
    var dObservableProp: Int by Delegates.observable(1) { _, _, _ -> }
    var dCustomDelegatedProp: String by StringDelegate()

    @get:JvmName("getRenamedNum")
    @DSName("renamedNum", "number")
    val aProp: Int = 1

    @get:JvmName("isActiveJvm")
    @DSName("active", "isActive")
    val aIsActive: Boolean = true

    val nOuterProp: String = ""
}
val GenericDestructuringObject<*, *, *>.eExtProp: String get() = ""

open class VisibilityCasesBase {
    internal open val vInternalOpenProp: Double = 1.0
    open val vPublicOpenProp: Boolean = true
    val vPublicFinalProp: Int = 1
}
open class VisibilityCasesChild : VisibilityCasesBase() {
    override val vInternalOpenProp: Double = 1.0
    override val vPublicOpenProp: Boolean = true
}

object ObjectSingletonProps {
    const val oConstProp: Int = 1
    @JvmField val oJvmFieldProp: String = ""
    val oValProp: String = ""
    var oVarProp: Int = 1
}

@JvmInline
value class ValueClassProps(val vValue: Int)

val common = DestructuringObject(pCProp = 1, pCNullableProp = null, pCVarProp = 2.5)
val generic = GenericDestructuringObject<Int, String, String>(pCPropOutO = "", pCInConsumer = { it.length })
val singleton = ObjectSingletonProps
val valueClass = ValueClassProps(1)
val visibilityChild = VisibilityCasesChild()

val delegatedCommon by lazy { DestructuringObject(pCProp = 1, pCNullableProp = "", pCVarProp = 3.14) }
val delegatedGeneric by lazy { GenericDestructuringObject<Int, String, String>(pCPropOutO = "", pCInConsumer = { it.length }) }
val delegatedSingleton by lazy { ObjectSingletonProps }
val delegatedValueClass by lazy { ValueClassProps(1) }
val delegatedVisibilityChild by lazy { VisibilityCasesChild() }

@JvmName("getCommonFun") fun getCommon() = common
@JvmName("getGenericFun") fun getGeneric() = generic
@JvmName("getSingletonFun") fun getSingleton() = singleton
@JvmName("getValueClassFun") fun getValueClass() = valueClass
@JvmName("getVisibilityChildFun") fun getVisibilityChild() = visibilityChild

fun destructuringFromTopLevelPositiveAliases() {
    (
    val pCProp: NBAliasInt,
    val pCNullableProp: NBAliasNullableString,
    val pCVarProp: Double,
    val pCDefaultProp: Boolean,
    val bProp: NBAliasInt,
    val bVarProp: NBAliasString,
    val bNullableProp: NBAliasNullableString,
    val cComputedOnly: NBAliasInt,
    val cComputedVar: NBAliasString,
    val dLazyProp: NBAliasString,
    val dObservableProp: NBAliasInt,
    val dCustomDelegatedProp: NBAliasString,
    ) = common

    (val num: NBAliasInt = aProp, val act: Boolean = aIsActive, val out: NBAliasString = nOuterProp, val ext: NBAliasString = eExtProp) = common

    (val constInt: NBAliasInt = oConstProp, val jvmF: NBAliasString = oJvmFieldProp) = singleton
    (val valP: NBAliasString = oValProp, val varP: NBAliasInt = oVarProp) = singleton

    (val vv: NBAliasInt = vValue) = valueClass
    (val io: Double = vInternalOpenProp, val po: Boolean = vPublicOpenProp, val pf: NBAliasInt = vPublicFinalProp) = visibilityChild

    (val n1: NBAliasNumber = pCProp, val n2: NBAliasNumber = bProp) = common

    jvmF.length + valP.length + ext.length + out.length + vv + pf + io.toInt() +
            pCProp + bProp + n1.toInt() + n2.toInt() + if (po && act) 1 else 0
}

fun destructuringFromFunctionsPositiveAliases() {
    (
    val pCProp: NBAliasInt, val pCNullableProp: NBAliasNullableString, val pCVarProp: Double, val pCDefaultProp: Boolean,
    val bProp: NBAliasInt, val bVarProp: NBAliasString, val bNullableProp: NBAliasNullableString,
    val cComputedOnly: NBAliasInt, val cComputedVar: NBAliasString,
    val dLazyProp: NBAliasString, val dObservableProp: NBAliasInt, val dCustomDelegatedProp: NBAliasString,
    val nOuterProp: NBAliasString,
    val aProp: NBAliasInt, val aIsActive: Boolean,
    ) = getCommon()

    (val oVal: NBAliasString = oValProp, val oJvm: NBAliasString = oJvmFieldProp) = getSingleton()
    (val vv: NBAliasInt = vValue) = getValueClass()
    (val po: Boolean = vPublicOpenProp, val pf: NBAliasInt = vPublicFinalProp) = getVisibilityChild()

    oVal.length + oJvm.length + vv + pf + if (po) 1 else 0 + aProp + dObservableProp
}

fun destructuringFromReceiversPositiveAliases() {
    common.apply {
        (
        val pCProp: NBAliasInt, val pCNullableProp: NBAliasNullableString, val pCVarProp: Double, val pCDefaultProp: Boolean,
        val bProp: NBAliasInt, val bVarProp: NBAliasString, val bNullableProp: NBAliasNullableString,
        val cComputedOnly: NBAliasInt, val cComputedVar: NBAliasString,
        val dLazyProp: NBAliasString, val dObservableProp: NBAliasInt, val dCustomDelegatedProp: NBAliasString,
        ) = this
        (val ext: NBAliasString = eExtProp, val out: NBAliasString = nOuterProp, val num: NBAliasInt = aProp, val act: Boolean = aIsActive) = this
        ext.length + out.length + num + if (act) 1 else 0
    }
}

fun destructuringFromDelegatedPositiveAliases() {
    (
    val pCProp: NBAliasInt, val pCNullableProp: NBAliasNullableString, val pCVarProp: Double, val pCDefaultProp: Boolean,
    val bProp: NBAliasInt, val bVarProp: NBAliasString, val bNullableProp: NBAliasNullableString,
    val cComputedOnly: NBAliasInt, val cComputedVar: NBAliasString,
    val dLazyProp: NBAliasString, val dObservableProp: NBAliasInt, val dCustomDelegatedProp: NBAliasString,
    ) = delegatedCommon

    (val ext: NBAliasString = eExtProp, val out: NBAliasString = nOuterProp, val num: NBAliasInt = aProp, val act: Boolean = aIsActive) = delegatedCommon
    (val j: NBAliasString = oJvmFieldProp, val v: NBAliasString = oValProp) = delegatedSingleton
    (val vv: NBAliasInt = vValue) = delegatedValueClass
    (val io: Double = vInternalOpenProp, val po: Boolean = vPublicOpenProp, val pf: NBAliasInt = vPublicFinalProp) = delegatedVisibilityChild

    j.length + v.length + vv + pf + io.toInt() + if (po && act) 1 else 0 + num + bProp + dObservableProp
}

fun destructuringGenericPositiveAliases_allForms() {
    (
    val pCDefaultProp: Boolean,
    val bProp: NBAliasInt, val bVarProp: NBAliasString, val bNullableProp: NBAliasNullableString,
    val cComputedOnly: NBAliasInt, val cComputedVar: NBAliasString,
    val dLazyProp: NBAliasString, val dObservableProp: NBAliasInt, val dCustomDelegatedProp: NBAliasString,
    ) = generic
    (val ext: NBAliasString = eExtProp, val out: NBAliasString = nOuterProp, val num: NBAliasInt = aProp, val act: Boolean = aIsActive) = generic

    (
    val pCDefaultPropFn: Boolean = pCDefaultProp,
    val bPropFn: NBAliasInt = bProp,
    val bVarPropFn: NBAliasString = bVarProp,
    val bNullablePropFn: NBAliasNullableString = bNullableProp,
    val cComputedOnlyFn: NBAliasInt = cComputedOnly,
    val cComputedVarFn: NBAliasString  = cComputedVar,
    val dLazyPropFn: NBAliasString = dLazyProp,
    val dObservablePropFn: NBAliasInt = dObservableProp,
    val dCustomDelegatedPropFn: NBAliasString = dCustomDelegatedProp,
    ) = getGeneric()

    (
    val pCDefaultPropDel: Boolean = pCDefaultProp,
    val bPropDel: NBAliasInt = bProp,
    val bVarPropDel: NBAliasString = bVarProp,
    val bNullablePropDel: NBAliasNullableString = bNullableProp,
    val cComputedOnlyDel: NBAliasInt = cComputedOnly,
    val cComputedVarDel: NBAliasString = cComputedVar,
    val dLazyPropDel: NBAliasString  = dLazyProp,
    val dObservablePropDel: NBAliasInt = dObservableProp,
    val dCustomDelegatedPropDel: NBAliasString  = dCustomDelegatedProp,
    ) = delegatedGeneric

    generic.apply {
        (val out2: NBAliasString = nOuterProp, val num2: NBAliasInt = aProp, val act2: Boolean = aIsActive) = this
        out2.length + num2 + if (act2) 1 else 0
    }

    out.length + ext.length + num + if (act) 1 else 0 +
            bProp + dObservableProp + bPropFn + dObservablePropFn + bPropDel + dObservablePropDel +
            (if (pCDefaultProp && pCDefaultPropFn && pCDefaultPropDel) 1 else 0)
}

/* GENERATED_FIR_TAGS: additiveExpression, andExpression, annotationDeclaration, annotationUseSiteTargetPropertyGetter,
assignment, classDeclaration, const, destructuringDeclaration, functionDeclaration, functionalType, getter, ifExpression,
in, integerLiteral, lambdaLiteral, localProperty, nullableType, objectDeclaration, operator, out, outProjection,
override, primaryConstructor, propertyDeclaration, propertyDelegate, propertyWithExtensionReceiver, setter,
starProjection, stringLiteral, thisExpression, typeAliasDeclaration, typeParameter, value, vararg */
