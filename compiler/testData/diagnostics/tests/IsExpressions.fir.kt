// FILE: KotlinFile.kt

fun test() {
   if (<!USELESS_IS_CHECK!>1 is Int<!>) {
     if (1 is Boolean) {

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
