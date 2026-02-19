// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE: +NameBasedDestructuring, +EnableNameBasedDestructuringShortForm
// WITH_STDLIB

import kotlin.properties.Delegates
import kotlin.reflect.KProperty

class StringDelegate {
    private var value: String = ""
    operator fun getValue(thisRef: Any?, p: KProperty<*>) = value
    operator fun setValue(thisRef: Any?, p: KProperty<*>, v: String) { value = v }
}

open class DestructuringObject(
    val pCProp: Int, val pCNullableProp: String?, var pCVarProp: Double, val pCDefaultProp: Boolean = true
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
object ObjectSingletonProps {
    const val oConstProp: Int = 1
    @JvmField val oJvmFieldProp: String = ""
    val oValProp: String = ""
    var oVarProp: Int = 2
}
@JvmInline value class ValueClassProps(val vValue: Int)
open class VisibilityCasesBase { open val vPublicOpenProp: Boolean = true; val vPublicFinalProp: Int = 1 }
class VisibilityCasesChild : VisibilityCasesBase()

private val common = DestructuringObject(1, null, 2.5)
private val singleton = ObjectSingletonProps
private val valueClass = ValueClassProps(1)
private val visibilityChild = VisibilityCasesChild()

@JvmName("getCommonFun") fun getCommon() = common
@JvmName("getSingletonFun") fun getSingleton() = singleton
@JvmName("getValueClassFun") fun getValueClass() = valueClass
@JvmName("getVisibilityChildFun") fun getVisibilityChild() = visibilityChild

fun forDestructuringFromFunctionsPositiveShort() {
    for ((
        pCProp, pCNullableProp, pCVarProp, pCDefaultProp,
        bProp, bVarProp, bNullableProp,
        cComputedOnly, cComputedVar,
        dLazyProp, dObservableProp, dCustomDelegatedProp,
    ) in listOf(getCommon())) { }

    for ((
        nOuterProp,
        lLateinitVar,
        aProp, aIsActive,
    ) in listOf(getCommon())) { }

    for ((oConstProp, oJvmFieldProp, oValProp, oVarProp) in listOf(getSingleton())) { }
    for ((vValue) in listOf(getValueClass())) { }
    for ((vPublicOpenProp, vPublicFinalProp) in listOf(getVisibilityChild())) { }
}

/* GENERATED_FIR_TAGS: assignment, classDeclaration, const, forLoop, functionDeclaration, getter, integerLiteral,
lambdaLiteral, lateinit, localProperty, nullableType, objectDeclaration, operator, primaryConstructor,
propertyDeclaration, propertyDelegate, setter, starProjection, stringLiteral, value */
