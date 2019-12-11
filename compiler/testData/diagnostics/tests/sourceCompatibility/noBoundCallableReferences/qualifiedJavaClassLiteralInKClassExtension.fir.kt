// !LANGUAGE: -BoundCallableReferences
// !DIAGNOSTICS: -UNCHECKED_CAST

import kotlin.reflect.KClass

val <T : Any> KClass<T>.java: Class<T> get() = null!!

val <T : Any> KClass<T>.javaObjectType: Class<T>
    get() {
        return java.<!UNRESOLVED_REFERENCE!>lang<!>.<!UNRESOLVED_REFERENCE!>Class<!>::class.<!INAPPLICABLE_CANDIDATE!>java<!> as Class<T>
    }
