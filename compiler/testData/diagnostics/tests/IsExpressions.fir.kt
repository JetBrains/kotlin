// FILE: KotlinFile.kt

fun test() {
   if (1 is Int) {
     if (1 is Boolean) {

     }
   }

   A.create() is A
   A.create() is A?

   <!UNRESOLVED_REFERENCE!>unresolved<!> is A
   <!UNRESOLVED_REFERENCE!>unresolved<!> is A?

   val x = foo()
   x as String
   x is String
}

fun foo(): Any = ""

// FILE: A.java
class A {
    static A create() { return null; }
}