// RUN_PIPELINE_TILL: CODEGEN
// LANGUAGE: +CompanionBlocks
// WITH_STDLIB
// DIAGNOSTICS: -UNUSED_PARAMETER

// Companion block statics with the JVM signature of a member inherited from a supertype
// (see accidentalOverride.kt for members and statics of a superclass).

// Interface default method.
interface WithDefault {
    fun foo() = "default"
}

class HidesDefault : WithDefault {
    companion {
        <!ACCIDENTAL_OVERRIDE!>fun foo()<!> = "static"
    }
}

// Bridge generated for an override of a generic member.
open class Generic<T> {
    open fun take(t: T) = "base"
}

class HidesBridge : Generic<String>() {
    override fun take(t: String) = "override"

    companion {
        <!ACCIDENTAL_OVERRIDE!>fun take(o: Any?)<!> = "static"
    }
}

// Final member of java.lang.Object.
class HidesFinalObjectMethod {
    companion {
        <!ACCIDENTAL_OVERRIDE!>fun notify()<!> {}
    }
}

// Instance member of the child vs companion block static of the parent with the same erasure.
open class StaticParent {
    companion {
        fun test(x: List<Int>) = "static"
    }
}

class InstanceChild : StaticParent() {
    <!ACCIDENTAL_OVERRIDE!>fun test(x: List<String>)<!> = "instance"
}

// Companion block static of the parent vs @JvmStatic member of the child's companion object.
class JvmStaticChild : StaticParent() {
    companion object {
        @JvmStatic
        <!ACCIDENTAL_OVERRIDE!>fun test(x: List<String>) = "object"<!>
    }
}

// No clash: interface statics are not inherited.
interface InterfaceWithStatic {
    companion {
        fun test(x: List<Int>) = "interface"
    }
}

class ImplementsInterface : InterfaceWithStatic {
    companion {
        fun test(x: List<String>) = "class"
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, companionObject, functionDeclaration, interfaceDeclaration, nullableType,
objectDeclaration, override, stringLiteral, typeParameter */
