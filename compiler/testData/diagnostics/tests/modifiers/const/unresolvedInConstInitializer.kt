// RUN_PIPELINE_TILL: FRONTEND
// DIAGNOSTICS: -UNUSED_PARAMETER

const val fromUndeclaredInsideConstOp: Int = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>60 * <!UNRESOLVED_REFERENCE!>undeclaredUnsigned<!><!>

const val fromUndeclaredMember: String = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>"abc".<!UNRESOLVED_REFERENCE!>undeclaredMember<!>()<!>

const val plusWithoutParameter: Int = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>1.<!NONE_APPLICABLE, NO_VALUE_FOR_PARAMETER!>plus<!>()<!>

const val plusOnWrongArgType: Int = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>1.<!NONE_APPLICABLE!>plus<!>("str")<!>

const val chainedUnresolvedMethod: Int = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>1.plus(2).<!UNRESOLVED_REFERENCE!>nonExistentMethod<!>()<!>

/* GENERATED_FIR_TAGS: const, multiplicativeExpression, propertyDeclaration, stringLiteral */
