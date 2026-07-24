// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-30062

// Note: in original example, it was written 'by b', despite the fact 'by a' is more correct.
// Nevertheless, both were failing with exception. I kept 'by b' to have consistency with the issue.
class B(val a: B): <!CYCLIC_INHERITANCE_HIERARCHY, DELEGATION_NOT_TO_INTERFACE!>B<!> by <!UNRESOLVED_REFERENCE!>b<!>

open class C(val d: D?) : <!CYCLIC_INHERITANCE_HIERARCHY, DELEGATION_NOT_TO_INTERFACE!>C<!> by d!!

class D : C(null)

typealias F = E

class E(val f: F) : <!CYCLIC_INHERITANCE_HIERARCHY, DELEGATION_NOT_TO_INTERFACE!>E<!> by f

typealias H = <!RECURSIVE_TYPEALIAS_EXPANSION!>G<!>

class G(val h: G) : <!CYCLIC_INHERITANCE_HIERARCHY!>H<!> by h

/* GENERATED_FIR_TAGS: classDeclaration, inheritanceDelegation, primaryConstructor, propertyDeclaration */
