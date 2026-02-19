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

class GenericDestructuringObject<T>(val pCDefaultProp: Boolean = true) {
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
val GenericDestructuringObject<*>.eExtProp: String get() = ""

val generic = GenericDestructuringObject<Int>()
val delegatedGeneric by lazy { GenericDestructuringObject<Int>() }

@JvmName("getGenericFun") fun getGeneric() = generic

fun destructuringGenericTopLevel() {
    generic.lLateinitVar = ""
    (
    val pCDefaultProp,
    val bProp, val bVarProp, val bNullableProp,
    val cComputedOnly, val cComputedVar,
    val dLazyProp, val dObservableProp, val dCustomDelegatedProp,
    ) = generic

    (val eExtProp, val nOuterProp, val aProp, val lLateinitVar, val aIsActive) = generic

    (val renamed = bVarProp, val _ = cComputedOnly, val num = aProp,) = generic

    bVarProp.length + (bNullableProp?.length ?: 0) + dLazyProp.length + cComputedVar.length +
            eExtProp.length + nOuterProp.length + lLateinitVar.length + num + bProp + pCDefaultProp.hashCode()
}

fun destructurinGenericFromFunction() {
    (val again = aProp) = getGeneric()
    again + 1
}

fun destructuringGenericFromReceiver() {
    generic.apply {
        (val fromThis = aProp) = this
        fromThis + 1
    }
    generic.let {
        (val fromIt = aProp) = it
        fromIt + 1
    }
}

fun destructuringGenericFromDelegated() {
    delegatedGeneric.lLateinitVar = ""
    (val a = aProp) = delegatedGeneric
    a + 1
}

/* GENERATED_FIR_TAGS: additiveExpression, assignment, classDeclaration, destructuringDeclaration, elvisExpression,
functionDeclaration, getter, integerLiteral, lambdaLiteral, lateinit, localProperty, nullableType, operator,
primaryConstructor, propertyDeclaration, propertyDelegate, propertyWithExtensionReceiver, safeCall, setter,
starProjection, stringLiteral, thisExpression, typeParameter, unnamedLocalVariable */
