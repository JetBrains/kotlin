// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +CompanionBlocksAndExtensions

@Deprecated("Object0", level = DeprecationLevel.HIDDEN)
object Object0 {
    operator fun invoke() { }
}

@Deprecated("CompanionBlock", level = DeprecationLevel.HIDDEN)
class CompanionBlock private constructor() {
    companion {
        operator fun invoke() { }
    }
}

@Deprecated("CompanionObject", level = DeprecationLevel.HIDDEN)
class CompanionObject private constructor() {
    companion object {
        operator fun invoke() { }
    }
}

class CompanionObject2 private constructor() {
    @Deprecated("CompanionObject2", level = DeprecationLevel.HIDDEN)
    companion object {
        operator fun invoke() { }
    }
}

open class SuperType
operator fun SuperType.invoke() { }

@Deprecated("CompanionObjectExtension", level = DeprecationLevel.HIDDEN)
class CompanionObjectExtension private constructor() {
    companion object : SuperType()
}

class CompanionObjectExtension2 private constructor() {
    @Deprecated("CompanionObjectExtension2", level = DeprecationLevel.HIDDEN)
    companion object : SuperType()
}

fun test() {
    <!UNRESOLVED_REFERENCE!>Object0<!>()
    <!DEPRECATION_ERROR, INVISIBLE_REFERENCE!>CompanionBlock<!>()
    <!DEPRECATION_ERROR, INVISIBLE_REFERENCE!>CompanionObject<!>()
    <!INVISIBLE_REFERENCE!>CompanionObject2<!>()
    <!DEPRECATION_ERROR, INVISIBLE_REFERENCE!>CompanionObjectExtension<!>()
    <!INVISIBLE_REFERENCE!>CompanionObjectExtension2<!>()
}

/* GENERATED_FIR_TAGS: classDeclaration, companionObject, funWithExtensionReceiver, functionDeclaration,
objectDeclaration, operator, primaryConstructor, stringLiteral */
