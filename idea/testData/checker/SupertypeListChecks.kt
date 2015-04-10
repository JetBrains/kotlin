// KT-286 Check supertype lists

/*
In a supertype list:
 Same type should not be mentioned twice
 Same type should not be indirectly mentioned with incoherent type arguments
 Every trait's required dependencies should be satisfied
 No final types should appear
 Only one class is allowed
*/

class C1()

open class OC1()

open class C2 {}

open class C3 {}

trait T1 {}

trait T2<T> {}

trait Test<error>()</error> {
}

trait Test1 : <warning>C2</warning><error>()</error> {}

trait Test2 : <warning>C2</warning> {}

trait Test3 : <warning>C2</warning>, <error>C3</error> {}

trait Test4 : T1 {}

trait Test5 : T1, <error>T1</error> {}

trait Test6 : <error>C1</error> {}

class CTest1() : OC1() {}

class CTest2 : <error>C2</error> {}

class CTest3 : <error>C2</error>, <error>C3</error> {}

class CTest4 : T1 {}

class CTest5 : T1, <error>T1</error> {}

class CTest6 : <error>C1</error> {}

