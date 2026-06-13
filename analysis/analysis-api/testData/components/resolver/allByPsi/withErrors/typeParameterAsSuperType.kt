package bar

interface MyInterface

class MyClass<T : MyInterface> {
   inner class Inner : T
}
