// !LANGUAGE: -BoundCallableReferences

import kotlin.reflect.KClass

val <T : Any> KClass<T>.java: Class<T> get() = null!!

val <T : Any> KClass<T>.foo: Any?
    get() {
        return java.<!UNRESOLVED_REFERENCE!>lang<!>.<!UNRESOLVED_REFERENCE!>Integer<!>::hashCode
    }
