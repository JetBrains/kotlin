abstract class A {
    abstract override fun toString(): String
}

interface B

abstract class C : A(), B

<!ABSTRACT_CLASS_MEMBER_NOT_IMPLEMENTED!>class Test<!> : C()
