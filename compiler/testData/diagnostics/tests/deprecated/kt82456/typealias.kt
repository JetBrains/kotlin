// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +CompanionBlocksAndExtensions
// LANGUAGE_FEATURE_TOGGLED: ReportDeprecationsOfClassifiersInImplicitInvokes

class Outer {
    fun test() {
        <!DEPRECATION_ERROR!>TObj<!>()
        <!DEPRECATION!>TCompanionObj<!>()
        <!DEPRECATION_ERROR!>TInterface<!>()
        <!INTERFACE_AS_FUNCTION!>TIH1<!>()
        <!UNRESOLVED_REFERENCE!>TIH2<!>()
        TIH3()
        TIH4()
    }

    @Deprecated("", level = DeprecationLevel.HIDDEN)
    typealias TIH1 = IH1
    interface IH1 {
        companion object {
            operator fun invoke() { }
        }
    }

    typealias TIH2 = IH2
    interface IH2 {
        @Deprecated("", level = DeprecationLevel.HIDDEN)
        companion object {
            operator fun invoke() { }
        }
    }

    @Deprecated("", level = DeprecationLevel.HIDDEN)
    typealias TIH3 = IH3
    interface IH3 {
        companion object {
            operator fun invoke() { }
        }
    }

    @Deprecated("", level = DeprecationLevel.HIDDEN)
    typealias TIH4 = IH4
    interface IH4 {
        @Deprecated("", level = DeprecationLevel.HIDDEN)
        companion object {
            operator fun invoke() { }
        }
    }
}

object Obj {
    operator fun invoke() { }
}

@Deprecated("", level = DeprecationLevel.ERROR)
typealias TObj = Obj

class CompanionObj constructor(param: String) {
    companion object {
        operator fun invoke() { }
    }
}

@Deprecated("", level = DeprecationLevel.WARNING)
typealias TCompanionObj = CompanionObj

interface Interface {
    @Deprecated("", level = DeprecationLevel.ERROR)
    companion object {
        operator fun invoke() { }
    }
}

typealias TInterface = Interface

fun TIH3() { }
fun TIH4() { }

/* GENERATED_FIR_TAGS: classDeclaration, companionObject, functionDeclaration, interfaceDeclaration, nestedClass,
objectDeclaration, operator, primaryConstructor, stringLiteral, typeAliasDeclaration */
