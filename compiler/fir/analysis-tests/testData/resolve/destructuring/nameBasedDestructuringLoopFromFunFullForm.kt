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

fun forDestructuringFromFunctionsPositive() {
    for ((
            val pCProp, val pCNullableProp, val pCVarProp, val pCDefaultProp,
    val bProp, val bVarProp, val bNullableProp,
    val cComputedOnly, val cComputedVar,
    val dLazyProp, val dObservableProp, val dCustomDelegatedProp,
    ) in listOf(getCommon())) { }

    for ((
    val nOuterProp,
    val lLateinitVar,
    val aProp, val aIsActive,
    ) in listOf(getCommon())) { }

    for ((val oConstProp, val oJvmFieldProp, val oValProp, val oVarProp) in listOf(getSingleton())) { }
    for ((val vValue) in listOf(getValueClass())) { }
    for ((val vPublicOpenProp, val vPublicFinalProp) in listOf(getVisibilityChild())) { }
}

/* GENERATED_FIR_TAGS: assignment, classDeclaration, const, forLoop, functionDeclaration, getter, integerLiteral,
lambdaLiteral, lateinit, localProperty, nullableType, objectDeclaration, operator, primaryConstructor,
propertyDeclaration, propertyDelegate, setter, starProjection, stringLiteral, value */
