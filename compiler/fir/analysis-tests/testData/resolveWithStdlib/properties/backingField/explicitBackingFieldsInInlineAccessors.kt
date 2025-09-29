// RUN_PIPELINE_TILL: BACKEND
// DIAGNOSTICS: -NOTHING_TO_INLINE
// ISSUE: KT-81245

class Test {
    val a: Any
        field: String  = ""

    var withInlineGetterSetter: Any
        inline get() {
            a.length
            return ""
        }
        inline set(value) {
            a.length
        }

    inline var inlineProperty: Any
        get() {
            a.length
            return ""
        }
        set(value) {
            a.length
        }
}

/* GENERATED_FIR_TAGS: classDeclaration, getter, propertyDeclaration, setter, smartcast, stringLiteral */
