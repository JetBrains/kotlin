package foo

annotation class AllOpen1
annotation class AllOpen2

@AllOpen1
class Base1
class Derived1 : Base1()

@AllOpen2
class Base2
class Derived2 : Base2()
