// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE: +NameBasedDestructuring, +EnableNameBasedDestructuringShortForm
// WITH_STDLIB

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
) {
    var bVarProp: String = ""
    var dCustomDelegatedProp: String by StringDelegate()
    var cComputedVar: String
        get() = ""
        set(@Suppress("UNUSED_PARAMETER") v) {}
    val aProp: Int = 1
    val aIsActive: Boolean = true
}
val DestructuringObject.eExtProp: String get() = ""

class DestructuringInForFromNestedReceiver(
    p: Int = 1, s: String? = null, v: Double = 0.0
) : DestructuringObject(p, s, v) {

    fun destructureFromThis() {
        for ((
            pCProp, pCNullableProp, pCVarProp,
            bVarProp, cComputedVar, dCustomDelegatedProp,
        ) in listOf(this)) { }

        for ((aIsActive, aProp,) in listOf(this)) { }

        for ((number = aProp, text = bVarProp) in listOf(this)) { }
    }

    inner class Inner {
        val aProp: Int = 1
        var bVarProp: String = ""

        fun destructureLabeledThis() {
            for ((aProp, bVarProp) in listOf(this)) { }

            for ((outerA = aProp, outerB = bVarProp) in listOf(this@DestructuringInForFromNestedReceiver)) { }
            for ((ext = eExtProp) in listOf(this@DestructuringInForFromNestedReceiver)) { }
        }
    }
}

/* GENERATED_FIR_TAGS: assignment, classDeclaration, forLoop, functionDeclaration, getter, inner, integerLiteral,
localProperty, nullableType, operator, primaryConstructor, propertyDeclaration, propertyDelegate,
propertyWithExtensionReceiver, setter, starProjection, stringLiteral, thisExpression */
