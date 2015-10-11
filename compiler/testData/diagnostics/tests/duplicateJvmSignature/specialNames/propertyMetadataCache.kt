// !DIAGNOSTICS: -UNUSED_PARAMETER

fun Any.getValue(x: Any?, y: Any): Any = null!!

class <!CONFLICTING_JVM_DECLARATIONS!>C<!> {
    val x by 1
    <!CONFLICTING_JVM_DECLARATIONS!>val `$propertyMetadata`: Array<PropertyMetadata><!> = null!!
}

val x by 1
<!CONFLICTING_JVM_DECLARATIONS!>val `$propertyMetadata`: Array<PropertyMetadata><!> = null!!