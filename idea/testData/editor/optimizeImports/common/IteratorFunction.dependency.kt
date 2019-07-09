package test1

class MyClass

operator fun MyClass.iterator(): Iterator<MyClass> {
     return object: Iterator<MyClass> {
         override fun next(): MyClass {
             throw Exception()
         }
         override fun hasNext(): Boolean {
             throw Exception()
         }
     }
 }