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

fun fetchGeneric() = generic

fun forDestructuringGenericFromTopLevelPositive() {
    for ((
            val pCDefaultProp,
    val bProp, val bVarProp, val bNullableProp,
    val cComputedOnly, val cComputedVar,
    val dLazyProp, val dObservableProp, val dCustomDelegatedProp,
    ) in listOf(generic)) { }

    for ((val eExtProp, val nOuterProp, val aProp, val aIsActive) in listOf(generic)) { }

    for ((val aIsActive, val aProp, val nOuterProp,) in listOf(generic)) { }

    for ((val number = aProp, val active = aIsActive, val out = nOuterProp) in listOf(generic)) { }
}

fun forDestructuringGenericFromFunctionsPositive() {
    for ((
            val pCDefaultProp,
    val bProp, val bVarProp, val bNullableProp,
    val cComputedOnly, val cComputedVar,
    val dLazyProp, val dObservableProp, val dCustomDelegatedProp,
    ) in listOf(fetchGeneric())) { }

    for ((val aProp, val aIsActive, val nOuterProp,) in listOf(fetchGeneric())) { }

    for ((val num = aProp, val act = aIsActive) in listOf(fetchGeneric())) { }
}

fun forDestructuringGenericFromReceiversPositive() {
    val base = generic

    base.apply {
        for ((
                val pCDefaultProp,
        val bProp, val bVarProp, val bNullableProp,
        val cComputedOnly, val cComputedVar,
        val dLazyProp, val dObservableProp, val dCustomDelegatedProp,
        ) in listOf(this)) { }

        for ((val eExtProp, val nOuterProp, val aProp, val aIsActive) in listOf(this)) { }

        for ((val aIsActive, val renamed = aProp, val nOuterProp,) in listOf(this)) { }
    }
}

fun forDestructuringGenericFromDelegatedPositive() {
    for ((
            val pCDefaultProp,
    val bProp, val bVarProp, val bNullableProp,
    val cComputedOnly, val cComputedVar,
    val dLazyProp, val dObservableProp, val dCustomDelegatedProp,
    ) in listOf(delegatedGeneric)) { }

    for ((val eExtProp, val nOuterProp, val aProp, val aIsActive) in listOf(delegatedGeneric)) { }

    for ((val nOuterProp, val aProp, val aIsActive,) in listOf(delegatedGeneric)) { }
}

/* GENERATED_FIR_TAGS: assignment, classDeclaration, forLoop, functionDeclaration, getter, integerLiteral, lambdaLiteral,
lateinit, localProperty, nullableType, operator, primaryConstructor, propertyDeclaration, propertyDelegate,
propertyWithExtensionReceiver, setter, starProjection, stringLiteral, thisExpression, typeParameter */
