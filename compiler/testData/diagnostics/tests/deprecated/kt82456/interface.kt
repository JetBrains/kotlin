// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +CompanionBlocksAndExtensions
// LANGUAGE_FEATURE_TOGGLED: ReportDeprecationsOfClassifiersInImplicitInvokes
// LANGUAGE_FEATURE_TOGGLED_IDENTICAL

class Outer {
    fun test() {
        IW()
        IE1()
        <!DEPRECATION_ERROR!>IE2<!>()
        <!INTERFACE_AS_FUNCTION!>IH1<!>()
        <!DEPRECATION_ERROR!>IH2<!>()
        IH3()
        <!DEPRECATION_ERROR!>IH4<!>()
    }

    @Deprecated("", level = DeprecationLevel.HIDDEN)
    interface IH1 {
        companion object {
            operator fun invoke() { }
        }
    }

    interface IH2 {
        @Deprecated("", level = DeprecationLevel.HIDDEN)
        companion object {
            operator fun invoke() { }
        }
    }

    @Deprecated("", level = DeprecationLevel.HIDDEN)
    interface IH3 {
        companion object {
            operator fun invoke() { }
        }
    }

    interface IH4 {
        @Deprecated("", level = DeprecationLevel.HIDDEN)
        companion object {
            operator fun invoke() { }
        }
    }
}

@Deprecated("", level = DeprecationLevel.WARNING)
interface IW {
    companion {
        operator fun invoke() { }
    }
}

@Deprecated("", level = DeprecationLevel.ERROR)
interface IE1 {
    companion object {
        operator fun invoke() { }
    }
}

interface IE2 {
    @Deprecated("", level = DeprecationLevel.ERROR)
    companion object {
        operator fun invoke() { }
    }
}

fun IH3() { }
fun IH4() { }

/* GENERATED_FIR_TAGS: classDeclaration, companionObject, functionDeclaration, interfaceDeclaration, nestedClass,
objectDeclaration, operator, stringLiteral */
