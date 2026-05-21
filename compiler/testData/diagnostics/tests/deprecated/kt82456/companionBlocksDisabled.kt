// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: -CompanionBlocksAndExtensions

class Outer {
    interface Hidden {
        @Deprecated("", level = DeprecationLevel.HIDDEN)
        companion object {
            operator fun invoke() { }
        }
    }

    class Hidden2(val p: Int) {
        @Deprecated("", level = DeprecationLevel.HIDDEN)
        companion object {
            operator fun invoke() { }
        }
    }

    class Hidden3(val p: Int) {
        @Deprecated("", level = DeprecationLevel.HIDDEN)
        companion object {
            operator fun invoke() { }
        }
    }

    fun test() {
        Hidden()
        Hidden2()
        <!NO_VALUE_FOR_PARAMETER!>Hidden3<!>()
    }
}

fun Hidden() { }
fun Hidden2() { }

/* GENERATED_FIR_TAGS: classDeclaration, companionObject, functionDeclaration, interfaceDeclaration, nestedClass,
objectDeclaration, operator, primaryConstructor, propertyDeclaration, stringLiteral */
