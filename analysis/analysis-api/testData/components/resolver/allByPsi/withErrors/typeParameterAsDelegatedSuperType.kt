package bar

interface MyInterface

class MyClass<T : MyInterface>(val i: T) {
   inner class Inner : T by i
}
