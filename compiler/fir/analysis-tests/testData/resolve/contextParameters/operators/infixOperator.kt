// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +ContextParameters

class A

context(a: A)
<!INAPPLICABLE_INFIX_MODIFIER!>infix<!> fun infixTopLevel(x: Int): Int {
    return x
}

context(a: A)
infix fun A.infixTopLevelExtension(x: Int): Int {
    return x
}

class X {
    context(a: A)
    infix fun infixMember(x: Int): Int {
        return x
    }

    context(a: A)
    infix fun A.infixMemberExtension(x: Int): Int {
        return x
    }

    fun usageInside() {
        with(A()) {
            X() infixMember 1
            A() infixMemberExtension 1
        }
        X() <!NO_CONTEXT_ARGUMENT!>infixMember<!> 1
        A() <!NO_CONTEXT_ARGUMENT!>infixMemberExtension<!> 1
    }
}

fun usageOutside() {
    with(A()) {
        X() infixMember 1
        A() infixTopLevelExtension 1
    }
    X() <!NO_CONTEXT_ARGUMENT!>infixMember<!> 1
    A() <!NO_CONTEXT_ARGUMENT!>infixTopLevelExtension<!> 1
}
