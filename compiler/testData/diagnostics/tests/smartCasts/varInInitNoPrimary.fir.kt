class Your {
    init {
        var y: String? = "xyz"
        if (y != null) {
            // Bug that should be fixed
            // Problem: descriptorToDeclaration cannot get here init block by its descriptor
            // See PreliminaryDeclarationVisitor.getVisitorByVariable
            y.hashCode()
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