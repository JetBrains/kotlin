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

interface Test<!CONSTRUCTOR_IN_INTERFACE!>()<!> {
}

interface Test1 : <!INTERFACE_WITH_SUPERCLASS!>C2<!><!SUPERTYPE_INITIALIZED_IN_INTERFACE!>()<!> {}

interface Test2 : <!INTERFACE_WITH_SUPERCLASS!>C2<!> {}

interface Test3 : <!INTERFACE_WITH_SUPERCLASS!>C2<!>, <!MANY_CLASSES_IN_SUPERTYPE_LIST!>C3<!> {}

interface Test4 : T1 {}

interface Test5 : T1, <!SUPERTYPE_APPEARS_TWICE!>T1<!> {}

interface Test6 : <!FINAL_SUPERTYPE, INTERFACE_WITH_SUPERCLASS!>C1<!> {}

class CTest1() : OC1() {}

class CTest2 : <!SUPERTYPE_NOT_INITIALIZED!>C2<!> {}

class CTest3 : <!SUPERTYPE_NOT_INITIALIZED!>C2<!>, <!MANY_CLASSES_IN_SUPERTYPE_LIST, SUPERTYPE_NOT_INITIALIZED!>C3<!> {}

class CTest4 : T1 {}

class CTest5 : T1, <!SUPERTYPE_APPEARS_TWICE!>T1<!> {}

class CTest6 : <!FINAL_SUPERTYPE, SUPERTYPE_NOT_INITIALIZED!>C1<!> {}