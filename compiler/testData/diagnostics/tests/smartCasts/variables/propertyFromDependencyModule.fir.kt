// RUN_PIPELINE_TILL: FRONTEND

// MODULE: direct
// FILE: Direct.kt
annotation class Ann

class Direct(
    val finalConstructorProperty: Any = "",
    @get:Ann val annotatedConstructorProperty: Any = "",
) {
    val finalClassProperty: Any = ""

    @get:Ann
    val annotatedClassProperty: Any = ""
}

// MODULE: app(direct)
class Same(
    val finalConstructorProperty: Any = "",
    @get:Ann val annotatedConstructorProperty: Any = "",
) {
    val finalClassProperty: Any = ""

    @get:Ann
    val annotatedClassProperty: Any = ""
}

fun isCast(s: Same, d: Direct) {
    if (s.finalConstructorProperty is String) {
        s.finalConstructorProperty.length
    }

    if (d.finalConstructorProperty is String) {
        <!SMARTCAST_IMPOSSIBLE!>d.finalConstructorProperty<!>.length
    }

    if (s.annotatedConstructorProperty is String) {
        s.annotatedConstructorProperty.length
    }

    if (d.annotatedConstructorProperty is String) {
        <!SMARTCAST_IMPOSSIBLE!>d.annotatedConstructorProperty<!>.length
    }

    if (s.finalClassProperty is String) {
        s.finalClassProperty.length
    }

    if (d.finalClassProperty is String) {
        <!SMARTCAST_IMPOSSIBLE!>d.finalClassProperty<!>.length
    }

    if (s.annotatedClassProperty is String) {
        s.annotatedClassProperty.length
    }

    if (d.annotatedClassProperty is String) {
        <!SMARTCAST_IMPOSSIBLE!>d.annotatedClassProperty<!>.length
    }
}

fun asCast(s: Same, d: Direct) {
    s.finalConstructorProperty as String
    s.finalConstructorProperty.length

    d.finalConstructorProperty as String
    <!SMARTCAST_IMPOSSIBLE!>d.finalConstructorProperty<!>.length

    s.annotatedConstructorProperty as String
    s.annotatedConstructorProperty.length

    d.annotatedConstructorProperty as String
    <!SMARTCAST_IMPOSSIBLE!>d.annotatedConstructorProperty<!>.length

    s.finalClassProperty as String
    s.finalClassProperty.length

    d.finalClassProperty as String
    <!SMARTCAST_IMPOSSIBLE!>d.finalClassProperty<!>.length

    s.annotatedClassProperty as String
    s.annotatedClassProperty.length

    d.annotatedClassProperty as String
    <!SMARTCAST_IMPOSSIBLE!>d.annotatedClassProperty<!>.length
}
