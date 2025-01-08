// RUN_PIPELINE_TILL: FRONTEND
// DIAGNOSTICS: -UNUSED_ANONYMOUS_PARAMETER
// LANGUAGE: +MultiPlatformProjects

// MODULE: common
// FILE: common.kt
import kotlin.<!UNRESOLVED_REFERENCE!>concurrent<!>.<!DEBUG_INFO_MISSING_UNRESOLVED!>Volatile<!>
import kotlin.<!UNRESOLVED_REFERENCE!>properties<!>.<!DEBUG_INFO_MISSING_UNRESOLVED!>Delegates<!>

class ConcurrentVolatile(<!VOLATILE_ON_VALUE{JVM}!>@<!UNRESOLVED_REFERENCE!>Volatile<!><!> val s: Int) {
    <!VOLATILE_ON_VALUE{JVM}!>@<!UNRESOLVED_REFERENCE!>Volatile<!><!> val x = 0
    // ok
    @<!UNRESOLVED_REFERENCE!>Volatile<!> var y = 1

    <!VOLATILE_ON_DELEGATE{JVM}!>@delegate:<!UNRESOLVED_REFERENCE!>Volatile<!><!> var z: String by <!UNRESOLVED_REFERENCE!>Delegates<!>.<!DEBUG_INFO_MISSING_UNRESOLVED!>observable<!>("?") { <!CANNOT_INFER_PARAMETER_TYPE!>prop<!>, <!CANNOT_INFER_PARAMETER_TYPE!>old<!>, <!CANNOT_INFER_PARAMETER_TYPE!>new<!> -> <!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>old<!>.<!DEBUG_INFO_MISSING_UNRESOLVED!>hashCode<!>() }

    <!VOLATILE_ON_VALUE{JVM}!>@field:<!UNRESOLVED_REFERENCE!>Volatile<!><!> val w = 2

    <!WRONG_ANNOTATION_TARGET{JVM}!>@<!UNRESOLVED_REFERENCE!>Volatile<!><!>
    var noBacking: String
        get() = ""
        set(value) {}
}

// MODULE: jvm()()(common)
// FILE: jvm.kt
import kotlin.jvm.Volatile as JvmVolatile
import kotlin.concurrent.Volatile
import kotlin.properties.Delegates

class ConcurrentVolatileOnJvm {
    <!VOLATILE_ON_VALUE!>@Volatile<!> val x = 0
    // ok
    @Volatile var y = 1

    <!VOLATILE_ON_DELEGATE!>@delegate:Volatile<!> var z: String by Delegates.observable("?") { prop, old, new -> old.hashCode() }

    <!VOLATILE_ON_VALUE!>@field:Volatile<!> val w = 2

    <!WRONG_ANNOTATION_TARGET!>@Volatile<!>
    var noBacking: String
        get() = ""
        set(value) {}
}

class JvmVolatile {
    <!VOLATILE_ON_VALUE!>@JvmVolatile<!> val x = 0
    // ok
    @JvmVolatile var y = 1

    <!VOLATILE_ON_DELEGATE!>@delegate:JvmVolatile<!> var z: String by Delegates.observable("?") { prop, old, new -> old.hashCode() }

    <!VOLATILE_ON_VALUE!>@field:JvmVolatile<!> val w = 2

    <!WRONG_ANNOTATION_TARGET!>@JvmVolatile<!>
    var noBacking: String
        get() = ""
        set(value) {}
}
