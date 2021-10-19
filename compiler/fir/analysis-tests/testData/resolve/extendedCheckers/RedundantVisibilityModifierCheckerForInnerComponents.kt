abstract class Foo {
    abstract var id: Int
        protected set

    protected open val a = "test"
}

class Bar : Foo() {
    override var id: Int = 1
        public set

    public override val a = "rest"
}

abstract class A1 {
    open var attribute = "a"
        protected set
}

class C1 : A1() {
    public override var attribute = super.attribute
}

fun test1() {
    C1().attribute = "c"
}

abstract class A2 {
    open var attribute = "a"
        protected set
}

class C2 : A2() {
    public override var attribute = super.attribute
        set
}

abstract class A3 {
    open var attribute = "a"
        protected set
}

abstract class B3 : A3() {
    override var attribute = "b"
}

class C3 : B3() {
    public override var attribute = super.attribute
}

abstract class A4 {
    open var attribute = "a"
        protected set
}

abstract class B4 : A4() {
    override var attribute = "b"
        set
}

class C4 : B4() {
    public override var attribute = super.attribute
}

abstract class A5 {
    open var attribute = "a"
        protected set
}

class B5 : A5() {
    override var attribute = "b"
        set
}

fun test5() {
    <!INVISIBLE_SETTER!>B5().attribute<!> = "c"
}
