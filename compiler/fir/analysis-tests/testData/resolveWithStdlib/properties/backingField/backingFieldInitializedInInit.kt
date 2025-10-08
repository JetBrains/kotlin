// RUN_PIPELINE_TILL: FRONTEND
// WITH_STDLIB

class Test {
    val numbers: List<Int> field: MutableList<Int>
    val names: List<String> <!EXPLICIT_FIELD_MUST_BE_INITIALIZED!>field: MutableList<String><!>

    init {
        numbers = mutableListOf(1, 2, 3)
    }
}

/* GENERATED_FIR_TAGS: assignment, classDeclaration, explicitBackingField, init, integerLiteral, propertyDeclaration */
