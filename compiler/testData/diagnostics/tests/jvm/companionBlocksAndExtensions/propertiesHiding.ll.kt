// LL_FIR_DIVERGENCE
// LL FIR diagnostics tests don't run the JVM backend, so its diagnostics are not reported: `ACCIDENTAL_OVERRIDE`.
// LL_FIR_DIVERGENCE
// RUN_PIPELINE_TILL: CODEGEN
// LANGUAGE: +CompanionBlocks

open class Base {
    companion {
        val valProp: List<Int> = emptyList()
        var varProp: List<Int> = emptyList()
        var propWithPrivateSetter: List<Int>
            get() = emptyList()
            private set(value) {}
    }
}

class HideSameTypeVal : Base() {
    companion {
        val valProp: List<Int> = emptyList()
        val varProp: List<Int> = emptyList()
        val propWithPrivateSetter: List<Int> = emptyList()
    }
}

class HideSameTypeVar : Base() {
    companion {
        var valProp: List<Int> = emptyList()
        var varProp: List<Int> = emptyList()
        var propWithPrivateSetter: List<Int> = emptyList()
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
