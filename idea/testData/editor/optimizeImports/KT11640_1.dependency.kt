package bug.a

class A(val foo: MyFunction)

class MyFunction
operator fun MyFunction.invoke() = println("invoke convention")
