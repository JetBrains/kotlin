// RUN_PIPELINE_TILL: BACKEND
// SKIP_TXT

// MODULE: transitive
// FILE: Transitive.kt
annotation class Ann

class Transitive(
    val finalConstructorProperty: Any = "",
    @get:Ann val annotatedConstructorProperty: Any = "",
) {
    val finalClassProperty: Any = ""

    @get:Ann
    val annotatedClassProperty: Any = ""
}


// MODULE: direct()()(transitive)
// FILE: Direct.kt
class Direct(
    val finalConstructorProperty: Any = "",
    @get:Ann val annotatedConstructorProperty: Any = "",
) {
    val finalClassProperty: Any = ""

    @get:Ann
    val annotatedClassProperty: Any = ""
}

// MODULE: app()()(direct)
class Same(
    val finalConstructorProperty: Any = "",
    @get:Ann val annotatedConstructorProperty: Any = "",
) {
    val finalClassProperty: Any = ""

    @get:Ann
    val annotatedClassProperty: Any = ""
}

fun isCast(s: Same, d: Direct, t: Transitive) {
    if (s.finalConstructorProperty is String) {
        <!DEBUG_INFO_SMARTCAST!>s.finalConstructorProperty<!>.length
    }

    if (d.finalConstructorProperty is String) {
        <!DEBUG_INFO_SMARTCAST!>d.finalConstructorProperty<!>.length
    }

    if (t.finalConstructorProperty is String) {
        <!DEBUG_INFO_SMARTCAST!>t.finalConstructorProperty<!>.length
    }

    if (s.annotatedConstructorProperty is String) {
        <!DEBUG_INFO_SMARTCAST!>s.annotatedConstructorProperty<!>.length
    }

    if (d.annotatedConstructorProperty is String) {
        <!DEBUG_INFO_SMARTCAST!>d.annotatedConstructorProperty<!>.length
    }

    if (t.annotatedConstructorProperty is String) {
        <!DEBUG_INFO_SMARTCAST!>t.annotatedConstructorProperty<!>.length
    }

    if (s.finalClassProperty is String) {
        <!DEBUG_INFO_SMARTCAST!>s.finalClassProperty<!>.length
    }

    if (d.finalClassProperty is String) {
        <!DEBUG_INFO_SMARTCAST!>d.finalClassProperty<!>.length
    }

    if (t.finalClassProperty is String) {
        <!DEBUG_INFO_SMARTCAST!>t.finalClassProperty<!>.length
    }

    if (s.annotatedClassProperty is String) {
        <!SMARTCAST_IMPOSSIBLE!>s.annotatedClassProperty<!>.length
    }

    if (d.annotatedClassProperty is String) {
        <!SMARTCAST_IMPOSSIBLE!>d.annotatedClassProperty<!>.length
    }

    if (t.annotatedClassProperty is String) {
        <!SMARTCAST_IMPOSSIBLE!>t.annotatedClassProperty<!>.length
    }
}

fun asCast(s: Same, d: Direct, t: Transitive) {
    s.finalConstructorProperty as String
    <!DEBUG_INFO_SMARTCAST!>s.finalConstructorProperty<!>.length

    d.finalConstructorProperty as String
    <!DEBUG_INFO_SMARTCAST!>d.finalConstructorProperty<!>.length

    t.finalConstructorProperty as String
    <!DEBUG_INFO_SMARTCAST!>t.finalConstructorProperty<!>.length

    s.annotatedConstructorProperty as String
    <!DEBUG_INFO_SMARTCAST!>s.annotatedConstructorProperty<!>.length

    d.annotatedConstructorProperty as String
    <!DEBUG_INFO_SMARTCAST!>d.annotatedConstructorProperty<!>.length

    t.annotatedConstructorProperty as String
    <!DEBUG_INFO_SMARTCAST!>t.annotatedConstructorProperty<!>.length

    s.finalClassProperty as String
    <!DEBUG_INFO_SMARTCAST!>s.finalClassProperty<!>.length

    d.finalClassProperty as String
    <!DEBUG_INFO_SMARTCAST!>d.finalClassProperty<!>.length

    t.finalClassProperty as String
    <!DEBUG_INFO_SMARTCAST!>t.finalClassProperty<!>.length

    s.annotatedClassProperty as String
    <!SMARTCAST_IMPOSSIBLE!>s.annotatedClassProperty<!>.length

    d.annotatedClassProperty as String
    <!SMARTCAST_IMPOSSIBLE!>d.annotatedClassProperty<!>.length

    t.annotatedClassProperty as String
    <!SMARTCAST_IMPOSSIBLE!>t.annotatedClassProperty<!>.length
}

/* GENERATED_FIR_TAGS: annotationDeclaration, annotationUseSiteTargetPropertyGetter, asExpression, classDeclaration,
functionDeclaration, ifExpression, isExpression, primaryConstructor, propertyDeclaration, smartcast, stringLiteral */
