// RUN_PIPELINE_TILL: BACKEND
class Your {
    init {
        var y: String? = "xyz"
        if (y != null) {
            // Bug that should be fixed
            // Problem: descriptorToDeclaration cannot get here init block by its descriptor
            // See PreliminaryDeclarationVisitor.getVisitorByVariable
            <!SMARTCAST_IMPOSSIBLE!>y<!>.hashCode()
        }
    }

    constructor()
}

class Normal {
    init {
        var y: String? = "xyz"
        if (y != null) {
            <!DEBUG_INFO_SMARTCAST!>y<!>.hashCode()
        }
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, equalityExpression, ifExpression, init, localProperty, nullableType,
propertyDeclaration, secondaryConstructor, smartcast, stringLiteral */
