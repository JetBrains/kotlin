// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL
// LANGUAGE: +SuspendFunctionAsSupertype
// SKIP_TXT
// DIAGNOSTICS: -CONFLICTING_INHERITED_MEMBERS, -CONFLICTING_OVERLOADS, -ABSTRACT_MEMBER_NOT_IMPLEMENTED, -FUN_INTERFACE_WRONG_COUNT_OF_ABSTRACT_MEMBERS

abstract class CSuper: () -> Unit

class C: <!MIXING_SUSPEND_AND_NON_SUSPEND_SUPERTYPES!>CSuper(), suspend () -> Unit<!> {
    override suspend fun invoke() {
    }
}

object O: <!MIXING_SUSPEND_AND_NON_SUSPEND_SUPERTYPES!>CSuper(), suspend () -> Unit<!> {
    override suspend fun invoke() {
    }
}

abstract class SCSuper: suspend () -> Unit

class C1: <!MIXING_SUSPEND_AND_NON_SUSPEND_SUPERTYPES!>SCSuper(), () -> Unit<!> {
    override suspend fun invoke() {
    }
}

object O1: <!MIXING_SUSPEND_AND_NON_SUSPEND_SUPERTYPES!>SCSuper(), () -> Unit<!> {
    override suspend fun invoke() {
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, functionalType, objectDeclaration, operator, override,
suspend */
