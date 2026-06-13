// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +CompanionBlocksAndExtensions
// LANGUAGE: +ReportDeprecationsOfClassifiersInImplicitInvokes

class A {
    @Deprecated("")
    object Obj {
        operator fun invoke() {}
    }

    @Deprecated("", level = DeprecationLevel.ERROR)
    interface CompanionBlock {
        companion {
            operator fun invoke() {}
        }
    }

    @Deprecated("", level = DeprecationLevel.HIDDEN)
    interface CompanionObject1 {
        companion object {
            operator fun invoke() {}
        }
    }

    interface CompanionObject2 {
        @Deprecated("", level = DeprecationLevel.HIDDEN)
        companion object {
            operator fun invoke() {}
        }
    }
}

fun test() {
    A.<!DEPRECATION!>Obj<!>()
    A.<!DEPRECATION_ERROR!>CompanionBlock<!>()
    A.<!INTERFACE_AS_FUNCTION!>CompanionObject1<!>()
    A.<!UNRESOLVED_REFERENCE!>CompanionObject2<!>()
}

/* GENERATED_FIR_TAGS: classDeclaration, companionObject, functionDeclaration, interfaceDeclaration, nestedClass,
objectDeclaration, operator, stringLiteral */
