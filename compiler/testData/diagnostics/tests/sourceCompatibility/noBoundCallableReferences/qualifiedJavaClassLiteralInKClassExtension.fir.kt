// !LANGUAGE: -BoundCallableReferences
// !DIAGNOSTICS: -UNCHECKED_CAST

import kotlin.reflect.KClass

val <T : Any> KClass<T>.java: Class<T> get() = null!!

val <T : Any> KClass<T>.javaObjectType: Class<T>
    get() {
        return java.<!UNRESOLVED_REFERENCE!>lang<!>.Class::class.<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>java<!> as Class<T>
    }
