// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE: +CompanionBlocksAndExtensions

@Deprecated("", level = DeprecationLevel.HIDDEN)
class A {
    companion {
        fun x() { }
        fun test1() { x() }
    }
    fun y() { }
    fun test2() { x(); y() }
}

@Deprecated("", level = DeprecationLevel.WARNING)
class B {
    companion {
        fun x() { }
        fun test1() { x() }
    }
    fun y() { }
    fun test2() { x(); y() }
}

@Deprecated("", level = DeprecationLevel.ERROR)
class C {
    companion object {
        fun x() { }
        fun test1() { x() }
    }
    fun test2() { x() }
}

class D {
    @Deprecated("", level = DeprecationLevel.ERROR)
    companion object {
        fun x() { }
        fun test1() { x() }
    }
    // questionable, probably should be reported;
    // however, consistency with `Hidden` level must be ensured:
    // error with ERROR level <=> skipped with HIDDEN level
    fun test2() { x() }
}

/* GENERATED_FIR_TAGS: classDeclaration, companionObject, functionDeclaration, objectDeclaration, stringLiteral */
