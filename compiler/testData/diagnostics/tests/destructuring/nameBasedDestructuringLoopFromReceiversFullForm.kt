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
open class VisibilityCasesBase { open val vInternalOpenProp: Double = 1.0; open val vPublicOpenProp: Boolean = true; val vPublicFinalProp: Int = 1 }
class VisibilityCasesChild : VisibilityCasesBase()

val common = DestructuringObject(1, null, 2.5)
val singleton = ObjectSingletonProps
val valueClass = ValueClassProps(1)
val visibilityChild = VisibilityCasesChild()

fun forDestructuringFromReceiversPositive() {
    val baseCommon = common

    baseCommon.apply {
        for ((
                val pCProp, val pCNullableProp, val pCVarProp, val pCDefaultProp,
        val bProp, val bVarProp, val bNullableProp,
        val cComputedOnly, val cComputedVar,
        val dLazyProp, val dObservableProp, val dCustomDelegatedProp,
        ) in listOf(this)) { }

        for ((val aIsActive, val aProp, val nOuterProp,) in listOf(this)) { }

        for ((val number = aProp, val text = bVarProp) in listOf(this)) { }

        lLateinitVar = ""
        for ((
                val eExtProp,
        val nOuterProp,
        val aProp, val aIsActive,
        val lLateinitVar,
        ) in listOf(this)) { }

        for ((val v = value, val i = index) in listOf(this).withIndex()) {
        i.inv()
        v.substring(1)
    }
    }

    singleton.apply {
        for ((val oConstProp, val oJvmFieldProp, val oValProp, val oVarProp) in listOf(this)) { }
    }

    valueClass.apply {
        for ((val vValue) in listOf(this)) { }
    }

    visibilityChild.apply {
        for ((val vInternalOpenProp, val vPublicOpenProp, val vPublicFinalProp) in listOf(this)) { }
    }
}

/* GENERATED_FIR_TAGS: assignment, classDeclaration, const, forLoop, funWithExtensionReceiver, functionDeclaration,
getter, integerLiteral, lambdaLiteral, lateinit, localProperty, nullableType, objectDeclaration, operator,
primaryConstructor, propertyDeclaration, propertyDelegate, propertyWithExtensionReceiver, setter, starProjection,
stringLiteral, thisExpression, value */
