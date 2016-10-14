// !LANGUAGE: -BoundCallableReferences
// !DIAGNOSTICS: -UNCHECKED_CAST

import kotlin.reflect.KClass

val <T : Any> KClass<T>.java: Class<T> get() = null!!

val <T : Any> KClass<T>.javaObjectType: Class<T>
    get() {
        return java.<!UNRESOLVED_REFERENCE!>lang<!>.<!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>Class<!>::class.<!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>java<!> <!USELESS_CAST!>as Class<T><!>
    }
