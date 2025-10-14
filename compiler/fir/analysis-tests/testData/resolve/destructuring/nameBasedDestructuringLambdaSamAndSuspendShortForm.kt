// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE: +NameBasedDestructuring, +EnableNameBasedDestructuringShortForm
// WITH_STDLIB

open class DestructuringObject(val aProp: Int = 1, val aIsActive: Boolean = true)
object ObjectSingletonProps { @JvmField val oJvmFieldProp: String = "" }

val common = DestructuringObject()
val singleton = ObjectSingletonProps

fun interface CommonConsumer { fun accept(o: DestructuringObject) }
fun interface TwoConsumer { fun accept(a: DestructuringObject, b: ObjectSingletonProps) }

fun acceptSamCommon(consumer: CommonConsumer) = consumer.accept(common)
fun acceptSamTwo(consumer: TwoConsumer) = consumer.accept(common, singleton)

fun acceptSuspendCommon(@Suppress("UNUSED_PARAMETER") block: suspend (DestructuringObject) -> Unit) {}
fun <T,I,O> acceptSuspendGeneric(@Suppress("UNUSED_PARAMETER") block: suspend (GenericDestructuringObject<T>) -> Unit) {}

class GenericDestructuringObject<T>(val pCDefaultProp: Boolean = true) {
    val bProp = 1; var bVarProp = ""; val bNullableProp: String? = null
}

fun lambdaParamsSamConversionPositiveShort() {
    acceptSamCommon { (aProp, aIsActive): DestructuringObject -> Unit }

    acceptSamCommon { (isActive = aIsActive, number = aProp): DestructuringObject -> Unit }

    acceptSamTwo { (a = aProp): DestructuringObject, (j = oJvmFieldProp): ObjectSingletonProps -> Unit }

    acceptSamCommon { (aIsActive, aProp,): DestructuringObject -> Unit }
}

suspend fun lambdaParamsSuspendPositiveShort() {
    acceptSuspendCommon { (aProp, aIsActive) -> Unit }

    acceptSuspendGeneric<Int, String, String> { (pCDefaultProp, bProp, bVarProp, bNullableProp) -> Unit }

    acceptSuspendCommon { (number = aProp) -> Unit }

    acceptSuspendCommon { (aIsActive, aProp,) -> Unit }
}

/* GENERATED_FIR_TAGS: classDeclaration, funInterface, functionDeclaration, functionalType, integerLiteral,
interfaceDeclaration, lambdaLiteral, localProperty, nullableType, objectDeclaration, primaryConstructor,
propertyDeclaration, samConversion, stringLiteral, suspend, typeParameter */
