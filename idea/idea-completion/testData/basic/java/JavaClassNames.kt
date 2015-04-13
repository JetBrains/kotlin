package testing

// Should show java names by short name
class Hello() {
   fun test() {
       val a : S<caret>
   }
}

// INVOCATION_COUNT: 2
// EXIST: SortedMap, Short, Socket
// ABSENT: hashSetOf