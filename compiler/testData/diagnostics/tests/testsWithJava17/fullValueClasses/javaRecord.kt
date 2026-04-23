// LANGUAGE: +FullValueClasses
// RUN_PIPELINE_TILL: FRONTEND
// WITH_STDLIB
// JVM_TARGET: 17

@JvmRecord
<!NON_FINAL_JVM_RECORD!>abstract<!> value class Abstract

@JvmRecord
<!NON_FINAL_JVM_RECORD!>sealed<!> value class Sealed

@JvmRecord
<!NON_FINAL_JVM_RECORD, VALUE_CLASS_OPEN!>open<!> value class Open

@JvmRecord
value class A(val x: Int, val y: Int)

<!JVM_RECORD_WITHOUT_PRIMARY_CONSTRUCTOR_PARAMETERS!>@JvmRecord<!>
<!WRONG_MODIFIER_TARGET!>value<!> object B

@JvmRecord
data class C(val x: Int, val y: Int)

<!JVM_RECORD_WITHOUT_PRIMARY_CONSTRUCTOR_PARAMETERS!>@JvmRecord<!>
data object D

<!NON_DATA_VALUE_CLASS_JVM_RECORD!>@JvmRecord<!>
class E(val x: Int, val y: Int)

<!NON_DATA_VALUE_CLASS_JVM_RECORD!>@JvmRecord<!>
object F

@JvmRecord
value class <!JVM_RECORD_EXTENDS_CLASS!>G<!>(val x: Int, val y: Int): <!SUPERTYPE_NOT_INITIALIZED, VALUE_CLASS_CANNOT_EXTEND_IDENTITY_CLASSES!>Any<!>

@JvmRecord
value class H(val x: Int, val y: Int): Comparable<H> {
    override fun compareTo(other: H): Int = 0
}

@JvmRecord
<!VALUE_CLASS_CANNOT_BE_CLONEABLE!>value<!> class <!JVM_RECORD_EXTENDS_CLASS!>I<!>(val x: Int, val y: Int): <!VALUE_CLASS_CANNOT_EXTEND_IDENTITY_CLASSES!>ArrayList<Int><!>()

abstract value class J
sealed value class J1
<!VALUE_CLASS_OPEN!>open<!> value class J2

value class ExplicitlyInheritingRecord(val x: Int): <!ILLEGAL_JAVA_LANG_RECORD_SUPERTYPE!>java.lang.Record<!>()

@JvmRecord
value class ExplicitlyInheritingRecordWithJvmRecordAnnotation(val x: Int): <!ILLEGAL_JAVA_LANG_RECORD_SUPERTYPE!>java.lang.Record<!>()

/* GENERATED_FIR_TAGS: classDeclaration, data, functionDeclaration, integerLiteral, objectDeclaration, operator,
override, primaryConstructor, propertyDeclaration, value */
