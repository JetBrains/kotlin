package foo.bar/* RootStructureElement *//* RootReplSnippetStructureElement */

class Builder {/* ClassDeclarationStructureElement */
    var version: String = ""/* DeclarationStructureElement */

    init {/* DeclarationStructureElement */
        val x = 1
    }

    fun execute() {/* DeclarationStructureElement */
        println(version)
    }

    class Nested {/* ClassDeclarationStructureElement */
        fun foo() {/* DeclarationStructureElement */}
        fun boo() {/* DeclarationStructureElement */}
    }
}

fun build(action: Builder.() -> Unit) = Builder().apply(action)/* DeclarationStructureElement */
fun build2(action: Builder.() -> Unit): Builder = Builder().apply(action)/* DeclarationStructureElement */

build {
    version = "123"
    class A {
        fun doo() {

        }
    }

    execute()
}

val builder = build {
    version = "321"
}/* DeclarationStructureElement */

builder.version = ""
builder.execute()
