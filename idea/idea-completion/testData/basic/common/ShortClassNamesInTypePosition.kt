// FIR_COMPARISON
package testing

class Hello() {
   fun test() {
       val a : S<caret>
   }
}

// EXIST: Set, Short, ShortArray
// ABSENT: toString
