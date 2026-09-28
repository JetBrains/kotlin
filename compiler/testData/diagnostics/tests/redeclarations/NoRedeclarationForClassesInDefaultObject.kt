// RUN_PIPELINE_TILL: CODEGEN
package kt2247

class B {
    companion object {
        class Y {
        }
    }

    class Y {
    }

}

/* GENERATED_FIR_TAGS: classDeclaration, companionObject, nestedClass, objectDeclaration */
