// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-28697
// WITH_STDLIB

// KT-28697: Report warning when the class property is shadowed by a local variable

fun main(args: Array<String>) = Foobar().print()

class Foobar {
    val name = "martin"

    fun print() {
        val name = "david" // local variable shadows class property
        println(name)
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, localProperty, propertyDeclaration, stringLiteral */
