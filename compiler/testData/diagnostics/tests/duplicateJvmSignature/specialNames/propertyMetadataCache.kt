// !DIAGNOSTICS: -UNUSED_PARAMETER -DEPRECATION

operator fun Any.getValue(x: Any?, y: Any): Any = null!!

class <!CONFLICTING_JVM_DECLARATIONS!>C<!> {
    val x by 1
    <!CONFLICTING_JVM_DECLARATIONS!>val `$delegatedProperties`: Array<PropertyMetadata><!> = null!!
}

val x by 1
<!CONFLICTING_JVM_DECLARATIONS!>val `$delegatedProperties`: Array<PropertyMetadata><!> = null!!