// !LANGUAGE: -BoundCallableReferences

import kotlin.reflect.KClass

val <T : Any> KClass<T>.java: Class<T> get() = null!!

val <T : Any> KClass<T>.foo: Any?
    get() {
        return <!UNSUPPORTED_FEATURE!>java.<!UNRESOLVED_REFERENCE!>lang<!>.<!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>Integer<!><!>::<!OVERLOAD_RESOLUTION_AMBIGUITY!>hashCode<!>
    }
