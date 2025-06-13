// RUN_PIPELINE_TILL: BACKEND
open class Base {
    companion object {
        val some = 0
    }
}

class Outer {
    val codegen = ""

    inner class Inner : Base() {
        val c = codegen
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, companionObject, inner, integerLiteral, objectDeclaration, propertyDeclaration,
stringLiteral */
