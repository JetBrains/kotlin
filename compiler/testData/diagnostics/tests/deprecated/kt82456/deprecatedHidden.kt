// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE: +CompanionBlocksAndExtensions

 // must resolve to constructors here
class Object
class CompanionBlock
class CompanionObject
class CompanionObject2
class CompanionObjectExtension
class CompanionObjectExtension2

class Outer {
    @Deprecated("Object", level = DeprecationLevel.HIDDEN)
    object Object {
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

    @Deprecated("CompanionObjectExtension", level = DeprecationLevel.HIDDEN)
    class CompanionObjectExtension private constructor() {
        companion object : SuperType()
    }

    class CompanionObjectExtension2 private constructor() {
        @Deprecated("CompanionObjectExtension2", level = DeprecationLevel.HIDDEN)
        companion object : SuperType()
    }

    operator fun SuperType.invoke() { }

    fun test() {
        Object()
        CompanionBlock()
        CompanionObject()
        CompanionObject2()
        CompanionObjectExtension()
        CompanionObjectExtension2()
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, companionObject, funWithExtensionReceiver, functionDeclaration, nestedClass,
objectDeclaration, operator, primaryConstructor, stringLiteral */
