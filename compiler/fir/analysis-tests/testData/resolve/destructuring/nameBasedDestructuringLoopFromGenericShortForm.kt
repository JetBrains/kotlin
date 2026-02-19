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

object ObjectSingletonProps {
    const val oConstProp = 1
    @JvmField val oJvmFieldProp = ""
    val oValProp = ""
    var oVarProp = 2
}

val generic = GenericDestructuringObject<Int>()
val delegatedGeneric by lazy { GenericDestructuringObject<Int>() }

// оставляем только fetchGeneric(), чтобы не конфликтовать с JVM-геттером свойства generic
fun fetchGeneric() = generic

inline fun <A, B> consumeTwo(a: A, b: B, f: (A, B) -> Unit) = f(a, b)
inline fun consumeTwo(f: (GenericDestructuringObject<Int>, ObjectSingletonProps) -> Unit) =
    f(generic, ObjectSingletonProps)

fun forDestructuringGenericFromTopLevelPositive() {
    for ((
        pCDefaultProp,
        bProp, bVarProp, bNullableProp,
        cComputedOnly, cComputedVar,
        dLazyProp, dObservableProp, dCustomDelegatedProp,
    ) in listOf(generic)) { }

    for ((eExtProp, nOuterProp, aProp, aIsActive) in listOf(generic)) { }

    for ((aIsActive, aProp, nOuterProp,) in listOf(generic)) { }

    for ((number = aProp, active = aIsActive, out = nOuterProp) in listOf(generic)) { }
}

fun forDestructuringGenericFromFunctionsPositive() {
    for ((
        pCDefaultProp,
        bProp, bVarProp, bNullableProp,
        cComputedOnly, cComputedVar,
        dLazyProp, dObservableProp, dCustomDelegatedProp,
    ) in listOf(fetchGeneric())) { }

    for ((aProp, aIsActive, nOuterProp,) in listOf(fetchGeneric())) { }

    for ((num = aProp, act = aIsActive) in listOf(fetchGeneric())) { }
}

fun forDestructuringGenericFromReceiversPositive() {
    val base = generic

    base.apply {
        for ((
            pCDefaultProp,
            bProp, bVarProp, bNullableProp,
            cComputedOnly, cComputedVar,
            dLazyProp, dObservableProp, dCustomDelegatedProp,
        ) in listOf(this)) { }

        for ((eExtProp, nOuterProp, aProp, aIsActive) in listOf(this)) { }

        for ((aIsActive, renamed = aProp, nOuterProp,) in listOf(this)) { }
    }
}

fun forDestructuringGenericFromDelegatedPositive() {
    for ((
        pCDefaultProp,
        bProp, bVarProp, bNullableProp,
        cComputedOnly, cComputedVar,
        dLazyProp, dObservableProp, dCustomDelegatedProp,
    ) in listOf(delegatedGeneric)) { }

    for ((eExtProp, nOuterProp, aProp, aIsActive) in listOf(delegatedGeneric)) { }

    for ((nOuterProp, aProp, aIsActive,) in listOf(delegatedGeneric)) { }
}

/* GENERATED_FIR_TAGS: assignment, classDeclaration, forLoop, functionDeclaration, getter, integerLiteral, lambdaLiteral,
lateinit, localProperty, nullableType, operator, primaryConstructor, propertyDeclaration, propertyDelegate,
propertyWithExtensionReceiver, setter, starProjection, stringLiteral, thisExpression, typeParameter */
