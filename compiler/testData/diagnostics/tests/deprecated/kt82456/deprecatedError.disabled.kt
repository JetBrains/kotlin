// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +CompanionBlocksAndExtensions
// LANGUAGE_FEATURE_TOGGLED: ReportDeprecationsOfClassifiersInImplicitInvokes

@Deprecated("Object", level = DeprecationLevel.ERROR)
object Object {
    operator fun invoke() { }
}

@Deprecated("CompanionBlock", level = DeprecationLevel.ERROR)
class CompanionBlock private constructor() {
    companion {
        operator fun invoke() { }
    }
}

@Deprecated("CompanionObject", level = DeprecationLevel.ERROR)
class CompanionObject private constructor() {
    companion object {
        operator fun invoke() { }
    }
}

class CompanionObject2 private constructor() {
    @Deprecated("CompanionObject2", level = DeprecationLevel.ERROR)
    companion object {
        operator fun invoke() { }
    }
}

open class SuperType
operator fun SuperType.invoke() { }

@Deprecated("CompanionObjectExtension", level = DeprecationLevel.ERROR)
class CompanionObjectExtension private constructor() {
    companion object : SuperType()
}

class CompanionObjectExtension2 private constructor() {
    @Deprecated("CompanionObjectExtension2", level = DeprecationLevel.ERROR)
    companion object : SuperType()
}

fun test() {
    <!DEPRECATION_ERROR_MIGRATION_PERIOD_WARNING!>Object<!>()
    <!DEPRECATION_ERROR_MIGRATION_PERIOD_WARNING!>CompanionBlock<!>()
    <!DEPRECATION_ERROR_MIGRATION_PERIOD_WARNING!>CompanionObject<!>()
    <!DEPRECATION_ERROR!>CompanionObject2<!>()
    <!DEPRECATION_ERROR_MIGRATION_PERIOD_WARNING!>CompanionObjectExtension<!>()
    <!DEPRECATION_ERROR!>CompanionObjectExtension2<!>()
}

/* GENERATED_FIR_TAGS: classDeclaration, companionObject, funWithExtensionReceiver, functionDeclaration,
objectDeclaration, operator, primaryConstructor, stringLiteral */
