// !DIAGNOSTICS: -UNUSED_VARIABLE -UNUSED_PARAMETER -TOPLEVEL_TYPEALIASES_ONLY

abstract class AbstractClass
typealias Test1 = AbstractClass
val test1 = <!CREATING_AN_INSTANCE_OF_ABSTRACT_CLASS!>Test1()<!>
val test1a = <!CREATING_AN_INSTANCE_OF_ABSTRACT_CLASS!>AbstractClass()<!>

annotation class AnnotationClass
typealias Test2 = AnnotationClass
val test2 = <!ANNOTATION_CLASS_CONSTRUCTOR_CALL!>Test2()<!>
val test2a = <!ANNOTATION_CLASS_CONSTRUCTOR_CALL!>AnnotationClass()<!>

enum class EnumClass { VALUE1, VALUE2 }
typealias Test3 = EnumClass
val test3 = <!ENUM_CLASS_CONSTRUCTOR_CALL!><!INVISIBLE_MEMBER!>Test3<!>()<!>
val test3a = <!ENUM_CLASS_CONSTRUCTOR_CALL!><!INVISIBLE_MEMBER!>EnumClass<!>()<!>

sealed class SealedClass
typealias Test4 = SealedClass
val test4 = <!SEALED_CLASS_CONSTRUCTOR_CALL!><!INVISIBLE_MEMBER!>Test4<!>()<!>
val test4a = <!SEALED_CLASS_CONSTRUCTOR_CALL!><!INVISIBLE_MEMBER!>SealedClass<!>()<!>

class Outer {
    inner class Inner
    typealias TestInner = Inner
}
typealias Test5 = Outer.Inner

val test5 = <!RESOLUTION_TO_CLASSIFIER!>Test5<!>()
val test5a = Outer.<!RESOLUTION_TO_CLASSIFIER!>Inner<!>()
val test5b = Outer.<!RESOLUTION_TO_CLASSIFIER!>TestInner<!>()
val test5c = Outer().<!UNRESOLVED_REFERENCE!>TestInner<!>()
val test5d = Outer().Inner()
val test5e = Outer().Test5()
