// RUN_PIPELINE_TILL: BACKEND
// WITH_STDLIB
// LANGUAGE: +NameBasedDestructuring, +EnableNameBasedDestructuringShortForm

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

fun destructuringFromTopLevelPositiveAliasesShort() {
    val (
        pCProp: NBAliasInt,
        pCNullableProp: NBAliasNullableString,
        pCVarProp: Double,
        pCDefaultProp: Boolean,
        bProp: NBAliasInt,
        bVarProp: NBAliasString,
        bNullableProp: NBAliasNullableString,
        cComputedOnly: NBAliasInt,
        cComputedVar: NBAliasString,
        dLazyProp: NBAliasString,
        dObservableProp: NBAliasInt,
        dCustomDelegatedProp: NBAliasString,
    ) = common

    val (num: NBAliasInt = aProp, act: Boolean = aIsActive, out: NBAliasString = nOuterProp, ext: NBAliasString = eExtProp) = common

    val (constInt: NBAliasInt = oConstProp, jvmF: NBAliasString = oJvmFieldProp) = singleton
    val (valP: NBAliasString = oValProp, varP: NBAliasInt = oVarProp) = singleton

    val (vv: NBAliasInt = vValue) = valueClass
    val (io: Double = vInternalOpenProp, po: Boolean = vPublicOpenProp, pf: NBAliasInt = vPublicFinalProp) = visibilityChild

    val (n1: NBAliasNumber = pCProp, n2: NBAliasNumber = bProp) = common

    jvmF.length + valP.length + ext.length + out.length + vv + pf + io.toInt() +
            pCProp + bProp + n1.toInt() + n2.toInt() + if (po && act) 1 else 0
}

fun destructuringFromFunctionsPositiveAliasesShort() {
    val (
        pCProp: NBAliasInt, pCNullableProp: NBAliasNullableString, pCVarProp: Double, pCDefaultProp: Boolean,
        bProp: NBAliasInt, bVarProp: NBAliasString, bNullableProp: NBAliasNullableString,
        cComputedOnly: NBAliasInt, cComputedVar: NBAliasString,
        dLazyProp: NBAliasString, dObservableProp: NBAliasInt, dCustomDelegatedProp: NBAliasString,
        nOuterProp: NBAliasString,
        aProp: NBAliasInt, aIsActive: Boolean,
    ) = getCommon()

    val (oVal: NBAliasString = oValProp, oJvm: NBAliasString = oJvmFieldProp) = getSingleton()
    val (vv: NBAliasInt = vValue) = getValueClass()
    val (po: Boolean = vPublicOpenProp, pf: NBAliasInt = vPublicFinalProp) = getVisibilityChild()

    oVal.length + oJvm.length + vv + pf + if (po) 1 else 0 + aProp + dObservableProp
}

fun destructuringFromReceiversPositiveAliasesShort() {
    common.apply {
        val (
            pCProp: NBAliasInt, pCNullableProp: NBAliasNullableString, pCVarProp: Double, pCDefaultProp: Boolean,
            bProp: NBAliasInt, bVarProp: NBAliasString, bNullableProp: NBAliasNullableString,
            cComputedOnly: NBAliasInt, cComputedVar: NBAliasString,
            dLazyProp: NBAliasString, dObservableProp: NBAliasInt, dCustomDelegatedProp: NBAliasString,
        ) = this
        val (ext: NBAliasString = eExtProp, out: NBAliasString = nOuterProp, num: NBAliasInt = aProp, act: Boolean = aIsActive) = this
        ext.length + out.length + num + if (act) 1 else 0
    }
}

fun destructuringFromDelegatedPositiveAliasesShort() {
    val (
        pCProp: NBAliasInt, pCNullableProp: NBAliasNullableString, pCVarProp: Double, pCDefaultProp: Boolean,
        bProp: NBAliasInt, bVarProp: NBAliasString, bNullableProp: NBAliasNullableString,
        cComputedOnly: NBAliasInt, cComputedVar: NBAliasString,
        dLazyProp: NBAliasString, dObservableProp: NBAliasInt, dCustomDelegatedProp: NBAliasString,
    ) = delegatedCommon

    val (ext: NBAliasString = eExtProp, out: NBAliasString = nOuterProp, num: NBAliasInt = aProp, act: Boolean = aIsActive) = delegatedCommon
    val (j: NBAliasString = oJvmFieldProp, v: NBAliasString = oValProp) = delegatedSingleton
    val (vv: NBAliasInt = vValue) = delegatedValueClass
    val (io: Double = vInternalOpenProp, po: Boolean = vPublicOpenProp, pf: NBAliasInt = vPublicFinalProp) = delegatedVisibilityChild

    j.length + v.length + vv + pf + io.toInt() + if (po && act) 1 else 0 + num + bProp + dObservableProp
}

fun destructuringGenericPositiveAliases_allFormsShort() {
    val (
        pCDefaultProp: Boolean,
        bProp: NBAliasInt, bVarProp: NBAliasString, bNullableProp: NBAliasNullableString,
        cComputedOnly: NBAliasInt, cComputedVar: NBAliasString,
        dLazyProp: NBAliasString, dObservableProp: NBAliasInt, dCustomDelegatedProp: NBAliasString,
    ) = generic
    val (ext: NBAliasString = eExtProp, out: NBAliasString = nOuterProp, num: NBAliasInt = aProp, act: Boolean = aIsActive) = generic

    val (
        pCDefaultPropFn: Boolean = pCDefaultProp,
    bPropFn: NBAliasInt = bProp,
    bVarPropFn: NBAliasString = bVarProp,
    bNullablePropFn: NBAliasNullableString = bNullableProp,
    cComputedOnlyFn: NBAliasInt = cComputedOnly,
    cComputedVarFn: NBAliasString  = cComputedVar,
    dLazyPropFn: NBAliasString = dLazyProp,
    dObservablePropFn: NBAliasInt = dObservableProp,
    dCustomDelegatedPropFn: NBAliasString = dCustomDelegatedProp,
    ) = getGeneric()

    val (
        pCDefaultPropDel: Boolean = pCDefaultProp,
    bPropDel: NBAliasInt = bProp,
    bVarPropDel: NBAliasString = bVarProp,
    bNullablePropDel: NBAliasNullableString = bNullableProp,
    cComputedOnlyDel: NBAliasInt = cComputedOnly,
    cComputedVarDel: NBAliasString = cComputedVar,
    dLazyPropDel: NBAliasString  = dLazyProp,
    dObservablePropDel: NBAliasInt = dObservableProp,
    dCustomDelegatedPropDel: NBAliasString  = dCustomDelegatedProp,
    ) = delegatedGeneric

    generic.apply {
        val (out2: NBAliasString = nOuterProp, num2: NBAliasInt = aProp, act2: Boolean = aIsActive) = this
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
