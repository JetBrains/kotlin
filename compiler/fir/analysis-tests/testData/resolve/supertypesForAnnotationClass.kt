interface SomeInterface

abstract class SomeClass

annotation class Ann : <!SUPERTYPES_FOR_ANNOTATION_CLASS!>SomeInterface, SomeClass()<!>

annotation class Ann2

annotation class Ann3 : <!SUPERTYPES_FOR_ANNOTATION_CLASS!>Annotation<!>

class MyClass : SomeInterface, SomeClass()
