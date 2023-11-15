// FIR_IDENTICAL
import kotlin.reflect.KClass

public annotation class Throws(vararg val exceptionClasses: KClass<out Throwable>)

typealias UEAlias = <!UNRESOLVED_REFERENCE!>UE<!>

@Throws(<!ANNOTATION_ARGUMENT_MUST_BE_CONST!>UEAlias::class<!>)
fun throwsTypealiasToUnresolved() {}
