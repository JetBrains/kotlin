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

interface <error descr="[CONSTRUCTOR_IN_INTERFACE] An interface may not have a constructor">Test()</error> {
}

interface Test1 : <error descr="[INTERFACE_WITH_SUPERCLASS] An interface cannot inherit from a class"><error descr="[SUPERTYPE_INITIALIZED_IN_INTERFACE] Interfaces cannot initialize supertypes">C2</error></error>() {}

interface Test2 : <error descr="[INTERFACE_WITH_SUPERCLASS] An interface cannot inherit from a class">C2</error> {}

interface Test3 : <error descr="[INTERFACE_WITH_SUPERCLASS] An interface cannot inherit from a class">C2</error>, <error descr="[MANY_CLASSES_IN_SUPERTYPE_LIST] Only one class may appear in a supertype list">C3</error> {}

interface Test4 : T1 {}

interface Test5 : T1, <error descr="[SUPERTYPE_APPEARS_TWICE] A supertype appears twice">T1</error> {}

interface Test6 : <error descr="[FINAL_SUPERTYPE] This type is final, so it cannot be inherited from"><error descr="[INTERFACE_WITH_SUPERCLASS] An interface cannot inherit from a class">C1</error></error> {}

class CTest1() : OC1() {}

class CTest2 : <error descr="[SUPERTYPE_NOT_INITIALIZED] This type has a constructor, and thus must be initialized here">C2</error> {}

class CTest3 : <error descr="[SUPERTYPE_NOT_INITIALIZED] This type has a constructor, and thus must be initialized here">C2</error>, <error descr="[MANY_CLASSES_IN_SUPERTYPE_LIST] Only one class may appear in a supertype list">C3</error> {}

class CTest4 : T1 {}

class CTest5 : T1, <error descr="[SUPERTYPE_APPEARS_TWICE] A supertype appears twice">T1</error> {}

class CTest6 : <error descr="[FINAL_SUPERTYPE] This type is final, so it cannot be inherited from"><error descr="[SUPERTYPE_NOT_INITIALIZED] This type has a constructor, and thus must be initialized here">C1</error></error> {}
