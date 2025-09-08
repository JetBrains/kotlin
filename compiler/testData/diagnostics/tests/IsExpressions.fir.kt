// RUN_PIPELINE_TILL: FRONTEND
// FILE: KotlinFile.kt

fun test() {
   if (<!USELESS_IS_CHECK!>1 is Int<!>) {
     if (<!IMPOSSIBLE_IS_CHECK_ERROR!>1 is Boolean<!>) {

     }
   }

   A.create() is A
   <!USELESS_IS_CHECK!>A.create() is A?<!>

   <!UNRESOLVED_REFERENCE!>unresolved<!> is A
   <!UNRESOLVED_REFERENCE!>unresolved<!> is A?

   val x = foo()
   x as String
   <!USELESS_IS_CHECK!>x is String<!>
}

fun foo(): Any = ""

// FILE: A.java
class A {
    static A create() { return null; }
}

/* GENERATED_FIR_TAGS: asExpression, flexibleType, functionDeclaration, ifExpression, integerLiteral, isExpression,
javaFunction, javaType, localProperty, nullableType, propertyDeclaration, smartcast, stringLiteral */
