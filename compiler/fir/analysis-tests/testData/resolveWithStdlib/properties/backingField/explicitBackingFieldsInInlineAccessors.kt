// RUN_PIPELINE_TILL: FRONTEND
// DIAGNOSTICS: -NOTHING_TO_INLINE
// ISSUE: KT-81245

class Test {
    val a: Any
        field: String  = ""

    var withInlineGetterSetter: Any
        inline get() {
            a.<!UNRESOLVED_REFERENCE!>length<!>
            return ""
        }
        inline set(value) {
            a.<!UNRESOLVED_REFERENCE!>length<!>
        }

    inline var inlineProperty: Any
        get() {
            a.<!UNRESOLVED_REFERENCE!>length<!>
            return ""
        }
        set(value) {
            a.<!UNRESOLVED_REFERENCE!>length<!>
        }
}

/* GENERATED_FIR_TAGS: classDeclaration, getter, propertyDeclaration, setter, smartcast, stringLiteral */
