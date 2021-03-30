// KT-286 Check supertype lists

/*
In a supertype list:
 Same type should not be mentioned twice
 Same type should not be indirectly mentioned with incoherent type arguments
 Every interface's required dependencies should be satisfied
 No final types should appear
 Only one class is allowed
*/

class C1()

open class OC1()

open class C2 {}

open class C3 {}

interface T1 {}

interface T2<T> {}

interface Test<error>()</error> {
}

interface Test1 : <error>C2</error><error>()</error> {}

interface Test2 : <error>C2</error> {}

interface Test3 : <error>C2</error>, <error>C3</error> {}

interface Test4 : T1 {}

interface Test5 : T1, <error>T1</error> {}

interface Test6 : <error>C1</error> {}

class CTest1() : OC1() {}

class CTest2 : <error>C2</error> {}

class CTest3 : <error>C2</error>, <error>C3</error> {}

class CTest4 : T1 {}

class CTest5 : T1, <error>T1</error> {}

class CTest6 : <error>C1</error> {}
