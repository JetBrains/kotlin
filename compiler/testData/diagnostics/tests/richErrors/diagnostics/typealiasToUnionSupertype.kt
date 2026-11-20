// RUN_PIPELINE_TILL: FRONTEND
error class E1
error class E2
interface I
open class Base

typealias IR = I | E1
typealias BaseR = Base | E1
typealias Errs = E1 | E2

class A : <!SUPERTYPE_NOT_A_CLASS_OR_INTERFACE!>IR<!>
class B : <!SUPERTYPE_NOT_A_CLASS_OR_INTERFACE!>BaseR<!>()
class D(i: I) : <!SUPERTYPE_NOT_A_CLASS_OR_INTERFACE!>IR<!> by i
object O : <!SUPERTYPE_NOT_A_CLASS_OR_INTERFACE!>IR<!>
val anon = object : <!SUPERTYPE_NOT_A_CLASS_OR_INTERFACE!>IR<!> {}
enum class En : <!SUPERTYPE_NOT_A_CLASS_OR_INTERFACE!>IR<!>
interface I2 : <!SUPERTYPE_NOT_A_CLASS_OR_INTERFACE!>IR<!>
class F : <!SUPERTYPE_NOT_A_CLASS_OR_INTERFACE!>Errs<!>
annotation class Ann : <!SUPERTYPES_FOR_ANNOTATION_CLASS, SUPERTYPE_NOT_A_CLASS_OR_INTERFACE!>Errs<!>

fun <T : IR> bound(x: T) {}
fun <T : Errs> errorBound(x: T) {}

/* GENERATED_FIR_TAGS: annotationDeclaration, anonymousObjectExpression, classDeclaration, enumDeclaration,
functionDeclaration, inheritanceDelegation, interfaceDeclaration, objectDeclaration, primaryConstructor,
propertyDeclaration, typeAliasDeclaration, typeConstraint, typeParameter */
