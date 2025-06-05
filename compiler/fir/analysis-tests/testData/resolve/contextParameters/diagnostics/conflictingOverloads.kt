// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +ContextParameters

// FILE: differentOrder.kt
package differentOrder

interface TypeA
interface TypeB

context(a: TypeA, b: TypeB) <!CONFLICTING_OVERLOADS!>fun foo()<!> {}
context(b: TypeB, a: TypeA) <!CONFLICTING_OVERLOADS!>fun foo()<!> {}

class C {
    context(a: TypeA, b: TypeB) <!CONFLICTING_OVERLOADS!>fun foo()<!> {}
    context(b: TypeB, a: TypeA) <!CONFLICTING_OVERLOADS!>fun foo()<!> {}
}

// FILE: subtypes.kt
package subtypes

interface SuperType
interface SubType : SuperType

context(sp: SuperType) fun foo() {}
context(sb: SubType) <!CONTEXTUAL_OVERLOAD_SHADOWED!>fun foo()<!> {}

class C {
    context(sp: SuperType) fun foo() {}
    context(sb: SubType) <!CONTEXTUAL_OVERLOAD_SHADOWED!>fun foo()<!> {}
}

// FILE: subset1.kt
package subset1

interface TypeA
interface TypeB

context(b: TypeB) fun foo() {}
context(a: TypeA, b: TypeB) <!CONTEXTUAL_OVERLOAD_SHADOWED!>fun foo()<!> {}

class C {
    context(a: TypeA, b: TypeB) fun foo() {}
    context(b: TypeB) fun foo() {}
}

// FILE: subset2.kt
package subset2

interface TypeA
interface TypeB

context(b: TypeA) fun foo() {}
context(a: TypeA, b: TypeB) <!CONTEXTUAL_OVERLOAD_SHADOWED!>fun foo()<!> {}

// FILE: subset3.kt
package subset3

interface TypeA
interface TypeB
interface TypeBSubType : TypeB

context(b: TypeB) fun foo() {}
context(a: TypeA, b: TypeBSubType) <!CONTEXTUAL_OVERLOAD_SHADOWED!>fun foo()<!> {}

// FILE: subset4.kt
package subset4

interface TypeA
interface TypeB
interface TypeASubType : TypeA

context(b: TypeA) fun foo() {}
context(a: TypeASubType, b: TypeB) <!CONTEXTUAL_OVERLOAD_SHADOWED!>fun foo()<!> {}
