// RUN_PIPELINE_TILL: FRONTEND
// WITH_STDLIB
// LANGUAGE: +NameBasedDestructuring

typealias NBAliasString = String
typealias NBAliasLong = Long

class DestructuringObject(val pCProp: Int, val pCNullableProp: String?)

object ObjectSingletonProps {
    const val oConstProp: Int = 1
}

@JvmInline
value class ValueClassProps(val vValue: Int)

class GenericDestructuringObject<T>(val bProp: Int = 1)

val common = DestructuringObject(pCProp = 1, pCNullableProp = null)
val singleton = ObjectSingletonProps
val valueClass = ValueClassProps(1)
val delegatedGeneric by lazy { GenericDestructuringObject<Int>() }


@JvmName("getSingletonFun")
fun getSingleton() = singleton


data class D(val ok: Int)
val D.<!EXTENSION_SHADOWED_BY_MEMBER!>ok<!>: String get() = "EXT"

fun negativeTopLevelCommon() {
    (val pCProp: <!INITIALIZER_TYPE_MISMATCH!>NBAliasString<!>) = common
    (val pCNullableProp: <!INITIALIZER_TYPE_MISMATCH!>NBAliasString<!>) = common
}

fun negativeFunctionSingleton() {
    (val oConstProp: <!INITIALIZER_TYPE_MISMATCH!>NBAliasString<!>) = getSingleton()
}

fun negativeReceiverValueclass() {
    valueClass.apply {
        (val vValue: <!INITIALIZER_TYPE_MISMATCH!>NBAliasString<!>) = this
    }
}

fun negativeDelegatedGeneric() {
    (val bProp: <!INITIALIZER_TYPE_MISMATCH!>NBAliasLong<!>) = delegatedGeneric
}

fun negativeMemberBeatsExtension() {
    val d = D(7)
    val (s: NBAliasString) = <!COMPONENT_FUNCTION_RETURN_TYPE_MISMATCH!>d<!>
}

/* GENERATED_FIR_TAGS: classDeclaration, const, data, destructuringDeclaration, functionDeclaration, getter,
integerLiteral, lambdaLiteral, localProperty, nullableType, objectDeclaration, primaryConstructor, propertyDeclaration,
propertyDelegate, propertyWithExtensionReceiver, stringLiteral, thisExpression, typeAliasDeclaration, typeParameter,
value */
