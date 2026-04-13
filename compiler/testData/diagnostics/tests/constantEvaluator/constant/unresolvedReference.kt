// RUN_PIPELINE_TILL: FRONTEND
import kotlin.reflect.KClass

public annotation class Throws(vararg val exceptionClasses: KClass<out Throwable>)

typealias UEAlias = <!UNRESOLVED_REFERENCE!>UE<!>

@Throws(<!ANNOTATION_ARGUMENT_MUST_BE_CONST!>UEAlias::class<!>)
fun throwsTypealiasToUnresolved() {}

/* GENERATED_FIR_TAGS: annotationDeclaration, classReference, functionDeclaration, outProjection, primaryConstructor,
propertyDeclaration, typeAliasDeclaration, vararg */
