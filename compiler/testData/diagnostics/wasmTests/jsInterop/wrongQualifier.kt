// FIR_IDENTICAL
// FILE: a.kt
@file:JsQualifier(<!WRONG_JS_QUALIFIER!>""<!>)

// FILE: b.kt
@file:JsQualifier(<!WRONG_JS_QUALIFIER!>"a..b"<!>)

// FILE: c.kt
@file:JsQualifier(<!WRONG_JS_QUALIFIER!>"a."<!>)

// FILE: d.kt
@file:JsQualifier(<!WRONG_JS_QUALIFIER!>".a"<!>)

// FILE: e.kt
@file:JsQualifier(<!WRONG_JS_QUALIFIER!>"%^&"<!>)

// FILE: f.kt
@file:JsQualifier("a.bc.d23._$")

// FILE: g.kt
typealias JsQ = JsQualifier

// FILE: h.kt
@file:JsQ(<!WRONG_JS_QUALIFIER!>value = "%^&"<!>)
