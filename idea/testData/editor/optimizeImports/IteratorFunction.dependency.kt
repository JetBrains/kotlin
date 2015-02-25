package test1

public class MyClass {
}

public fun MyClass.iterator(): Iterator<MyClass> {
     return object: Iterator<MyClass> {
         override fun next(): MyClass {
             throw Exception()
         }
         override fun hasNext(): Boolean {
             throw Exception()
         }
     }
 }