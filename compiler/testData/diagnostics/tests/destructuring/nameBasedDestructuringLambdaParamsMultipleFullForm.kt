// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE: +NameBasedDestructuring
// WITH_STDLIB

import kotlin.properties.Delegates
import kotlin.reflect.KProperty

class StringDelegate { private var v=""; operator fun getValue(r:Any?,p:KProperty<*>)=v; operator fun setValue(r:Any?,p:KProperty<*>,nv:String){v=nv} }

class GenericDestructuringObject<T>(val pCDefaultProp: Boolean = true) {
    val bProp = 1
    var bVarProp = ""
    val bNullableProp: String? = null
    val cComputedOnly get() = 1
    var cComputedVar: String
        get() = ""
        set(@Suppress("UNUSED_PARAMETER") v) {}
    val dLazyProp by lazy { "" }
    var dObservableProp: Int by Delegates.observable(1) { _,_,_ -> }
    var dCustomDelegatedProp: String by StringDelegate()
    val nOuterProp = ""
    val aProp = 1
    val aIsActive = true
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
val delegatedSingleton by lazy { ObjectSingletonProps }

inline fun <A,B> consumeTwo(a: A, b: B, f: (A,B)->Unit) = f(a,b)
inline fun consumeTwo(f: (GenericDestructuringObject<Int>, ObjectSingletonProps) -> Unit) =
    f(generic, delegatedSingleton)

fun lambdaParamsMultiplePositiveFull() {
    consumeTwo { (val aProp), (val oConstProp) -> Unit }

    consumeTwo(generic, delegatedSingleton) { (val aIsActive, val aProp,), (val oJvmFieldProp,) -> Unit }

    consumeTwo(generic, delegatedSingleton) { (val number = aProp), (val constN = oConstProp) -> Unit }

    consumeTwo(generic, delegatedSingleton) { (val eExtProp), (val v = oValProp, val w = oVarProp) -> Unit }
}

/* GENERATED_FIR_TAGS: assignment, classDeclaration, const, functionDeclaration, functionalType, getter, inline,
integerLiteral, lambdaLiteral, lateinit, localProperty, nullableType, objectDeclaration, operator, primaryConstructor,
propertyDeclaration, propertyDelegate, propertyWithExtensionReceiver, setter, starProjection, stringLiteral,
typeParameter */
