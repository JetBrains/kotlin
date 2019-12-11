class Your {
    init {
        var y: String? = "xyz"
        if (<!UNRESOLVED_REFERENCE!>y<!> != null) {
            // Bug that should be fixed
            // Problem: descriptorToDeclaration cannot get here init block by its descriptor
            // See PreliminaryDeclarationVisitor.getVisitorByVariable
            <!UNRESOLVED_REFERENCE!>y<!>.<!UNRESOLVED_REFERENCE!>hashCode<!>()
        }
    }

    constructor()
}

class Normal {
    init {
        var y: String? = "xyz"
        if (y != null) {
            y.hashCode()
        }
    }
}