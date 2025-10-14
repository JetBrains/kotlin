// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE: +NameBasedDestructuring
// WITH_STDLIB

import kotlin.reflect.KProperty

class StringDelegate {
    private var v = ""
    operator fun getValue(r: Any?, p: KProperty<*>) = v
    operator fun setValue(r: Any?, p: KProperty<*>, nv: String) { v = nv }
}

open class DestructuringObject(
    val aProp: Int = 1,
    val aIsActive: Boolean = true
) {
    var bVarProp = ""
    var dCustomDelegatedProp: String by StringDelegate()
    var cComputedVar: String
        get() = ""
        set(@Suppress("UNUSED_PARAMETER") v) {}
}
val DestructuringObject.eExtProp: String get() = ""

inline fun <T> consumeT(x: T, f: (T) -> Unit) = f(x)

class LambdaFromNestedReceiver(p: Int = 1) : DestructuringObject(p, true) {

    fun destructureFromThis() {
        consumeT(this) { (
            val cComputedVar,
            val aProp,
            val bVarProp,
            val dCustomDelegatedProp,
            ) -> Unit }

        consumeT(this) { (val number = aProp, val text = bVarProp) -> Unit }

        consumeT(this) { (val isActive = aIsActive) -> Unit }
    }

    inner class Inner {
        val aProp: Int = 1
        var bVarProp: String = ""

        fun destructureLabeledThis() {
            consumeT(this) { (val aProp, val bVarProp) -> Unit }

            consumeT(this@LambdaFromNestedReceiver) { (val outerA = aProp, val outerB = bVarProp) -> Unit }
            consumeT(this@LambdaFromNestedReceiver) { (val ext = eExtProp) -> Unit }
        }
    }
}

/* GENERATED_FIR_TAGS: assignment, classDeclaration, functionDeclaration, functionalType, getter, inline, inner,
integerLiteral, lambdaLiteral, localProperty, nullableType, operator, primaryConstructor, propertyDeclaration,
propertyDelegate, propertyWithExtensionReceiver, setter, starProjection, stringLiteral, thisExpression, typeParameter */
