// RUN_PIPELINE_TILL: CODEGEN
// LANGUAGE: +CompanionBlocks

open class Base {
    companion {
        val valProp: List<Number> = emptyList()
        var varProp: List<Number> = emptyList()
        var propWithPrivateSetter: List<Number>
            get() = emptyList()
            private set(value) {}
    }
}

class HideSameTypeVal : Base() {
    companion {
        val valProp: List<Number> = emptyList()
        val varProp: List<Number> = emptyList()
        val propWithPrivateSetter: List<Number> = emptyList()
    }
}

class HideSameTypeVar : Base() {
    companion {
        var valProp: List<Number> = emptyList()
        var varProp: List<Number> = emptyList()
        var propWithPrivateSetter: List<Number> = emptyList()
    }
}

class HideSameErasureVal : Base() {
    companion {
        val valProp: List<String> = emptyList()
        val varProp: List<String> = emptyList()
        val propWithPrivateSetter: List<String> = emptyList()
    }
}

class HideSameErasureVar : Base() {
    companion {
        var valProp: List<String> = emptyList()
        var varProp: List<String> = emptyList()
        var propWithPrivateSetter: List<String> = emptyList()
    }
}

class HideSubtypeVal : Base() {
    companion {
        val valProp: List<Int> = emptyList()
        val varProp: List<Int> = emptyList()
        val propWithPrivateSetter: List<Int> = emptyList()
    }
}

class HideSubtypeVar : Base() {
    companion {
        var valProp: List<Int> = emptyList()
        var varProp: List<Int> = emptyList()
        var propWithPrivateSetter: List<Int> = emptyList()
    }
}

class HideDifferentTypeVal : Base() {
    companion {
        val valProp: String = ""
        val varProp: String = ""
        val propWithPrivateSetter: String = ""
    }
}

class HideDifferentTypeVar : Base() {
    companion {
        var valProp: String = ""
        var varProp: String = ""
        var propWithPrivateSetter: String = ""
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, getter, integerLiteral, propertyDeclaration, setter, stringLiteral */
