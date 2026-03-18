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

    private inline var privateInlineProperty: Any
        get() {
            a.length
            return ""
        }
        set(value) {
            a.length
        }

    inline var privateInlineSetProperty: Any
        get() {
            a.<!UNRESOLVED_REFERENCE!>length<!>
            return ""
        }
        private set(value) {
            a.length
        }
}

/* GENERATED_FIR_TAGS: classDeclaration, explicitBackingField, getter, propertyDeclaration, setter, smartcast,
stringLiteral */
