// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE: +NameBasedDestructuring
// WITH_STDLIB

import kotlin.jvm.JvmName

open class DestructuringObject(val aProp: Int = 1, val aIsActive: Boolean = true)
object ObjectSingletonProps { const val oConstProp = 1; @JvmField val oJvmFieldProp = ""; val oValProp = ""; var oVarProp = 2 }
@JvmInline value class ValueClassProps(val vValue: Int)
open class VisibilityCasesBase { open val vPublicOpenProp = true; val vPublicFinalProp = 1 }
class VisibilityCasesChild : VisibilityCasesBase()

val common = DestructuringObject()
val singleton = ObjectSingletonProps
val valueClass = ValueClassProps(1)
val visibilityChild = VisibilityCasesChild()

@JvmName("getCommonFun") fun getCommon() = common
@JvmName("getSingletonFun") fun getSingleton() = singleton
@JvmName("getValueClassFun") fun getValueClass() = valueClass
@JvmName("getVisibilityChildFun") fun getVisibilityChild() = visibilityChild

inline fun <A, B> consumeTwo(a: A, b: B, f: (A, B) -> Unit) = f(a, b)

fun lambdaParamsFromFunctionsPositiveFull() {
    consumeTwo(getCommon(), getSingleton()) { (val aProp, val aIsActive), (val oConstProp, val oJvmFieldProp) -> Unit }

    consumeTwo(getCommon(), getSingleton()) { (val number = aProp), (val valP = oValProp, val varP = oVarProp) -> Unit }

    getValueClass().let { (val vValue) -> Unit }

    getVisibilityChild().let { (val vPublicOpenProp, val vPublicFinalProp) -> Unit }
}

/* GENERATED_FIR_TAGS: classDeclaration, const, functionDeclaration, functionalType, inline, integerLiteral,
lambdaLiteral, localProperty, nullableType, objectDeclaration, primaryConstructor, propertyDeclaration, stringLiteral,
typeParameter, value */
