// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +CompanionBlocksAndExtensions
class C {
    companion {
        fun foo() = ""
        val bar = <!PROPERTY_INITIALIZER_NO_BACKING_FIELD!>""<!>
    }

    companion object {
        fun foo() = 1
        val bar = 1
    }

    fun test() {
        val x: String = foo()
        val y: String = bar
    }
}

fun test() {
    val x: String = C.foo()
    val y: String = C.bar
}

/* GENERATED_FIR_TAGS: classDeclaration, companionObject, functionDeclaration, integerLiteral, localProperty,
objectDeclaration, propertyDeclaration, stringLiteral */
