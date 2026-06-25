// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +CompanionBlocksAndExtensions

class C {
    companion {
        val foo: List<Int>
            field: MutableList<Int> = TODO()

        fun insideCompanion() {
            foo.add(1)
        }
    }

    companion object {
       fun insideCompanionObject() {
            foo.add(1)
        }
    }

    fun insideClass() {
        foo.add(1)
    }
}

fun test() {
    C.foo.<!UNRESOLVED_REFERENCE!>add<!>(1)
}

/* GENERATED_FIR_TAGS: classDeclaration, explicitBackingField, functionDeclaration, propertyDeclaration */
