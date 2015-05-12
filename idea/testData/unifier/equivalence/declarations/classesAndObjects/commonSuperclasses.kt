fun foo() {
    open class Z(p: Int)
    interface T;

    {
        <selection>class A: Z(1), T</selection>
    }

    {
        class B: Z(1), T
    }

    {
        class C: Z(1)
    }

    {
        class D: Z(2), T
    }

    {
        class E: T
    }
}