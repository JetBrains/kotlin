// RUN_PIPELINE_TILL: BACKEND
// WITH_STDLIB
// LANGUAGE: +NameBasedDestructuring

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
) {
    var bVarProp: String = ""
    var dCustomDelegatedProp: String by StringDelegate()
    var cComputedVar: String
        get() = ""
        set(@Suppress("UNUSED_PARAMETER") v) {}
    val aProp: Int = 1
    val aIsActive: Boolean = true
    val eExtProp: String = "memberWins"
}
val DestructuringObject.<!EXTENSION_SHADOWED_BY_MEMBER!>eExtProp<!>: String get() = "extension"

class DestructuringFromNestedReceiver(
    p: Int = 1, s: String? = null, v: Double = 0.0
) : DestructuringObject(p, s, v) {

    fun destructureFromThis() {
        (val aProp, val bVarProp, val dCustomDelegatedProp, val cComputedVar) = this
        (val text = bVarProp, val _ = cComputedVar, val num = aProp) = this
        (val only = aProp,) = this
        text.length + num + only
    }

    inner class Inner {
        val aProp: Int = 1
        var bVarProp: String = ""

        fun destructureLabeledThis() {
            (val aProp, val bVarProp) = this
            (val outerA = aProp, val outerB = bVarProp) = this@DestructuringFromNestedReceiver
            (val ext = eExtProp) = this@DestructuringFromNestedReceiver
            ext.length + outerA + outerB.length + aProp + bVarProp.length
        }

        fun destructureItLambdaOnOuter() {
            this@DestructuringFromNestedReceiver.let {
                (val number = aProp, val isActive = aIsActive) = it
                number + (if (isActive) 1 else 0)
            }
        }

        fun destructureThisInnerExplicit() {
            this.run {
                (val innerA = aProp, val innerB = bVarProp) = this
                innerA + innerB.length
            }
        }
    }
}

/* GENERATED_FIR_TAGS: additiveExpression, assignment, classDeclaration, destructuringDeclaration, functionDeclaration,
getter, ifExpression, inner, integerLiteral, lambdaLiteral, localProperty, nullableType, operator, primaryConstructor,
propertyDeclaration, propertyDelegate, propertyWithExtensionReceiver, setter, starProjection, stringLiteral,
thisExpression, unnamedLocalVariable */
