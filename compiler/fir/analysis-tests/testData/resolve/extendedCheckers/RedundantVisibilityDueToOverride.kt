abstract class A {
    open var attribute = "a"
        protected set
}

class C1 : A() {
    <!REDUNDANT_VISIBILITY_MODIFIER!>public<!> override var attribute = super.attribute
        public set
}

abstract class B2 : A() {
    override var attribute = "b"
        public set
}

class C2 : B2() {
    <!REDUNDANT_VISIBILITY_MODIFIER!>public<!> override var attribute = super.attribute
}
