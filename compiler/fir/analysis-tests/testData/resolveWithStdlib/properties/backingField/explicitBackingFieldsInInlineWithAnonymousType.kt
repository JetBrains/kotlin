// RUN_PIPELINE_TILL: FRONTEND
// DIAGNOSTICS: -NOTHING_TO_INLINE

class AnonymousType {
    val anonymousType: Any
        field = object {
            val x: Int = 1
        }

    inline fun publicInline() {
        anonymousType.<!UNRESOLVED_REFERENCE!>x<!>.inc()
    }
    
    private inline fun privateInline() {
        anonymousType.x.inc()
    }

    public inline fun outer() {
        val local = object {
            private inline fun inner() {
                anonymousType.<!UNRESOLVED_REFERENCE!>x<!>.inc()
            }
        }
    }
}

/* GENERATED_FIR_TAGS: anonymousObjectExpression, classDeclaration, explicitBackingField, functionDeclaration, inline,
integerLiteral, localProperty, propertyDeclaration, smartcast */
