fun foo() {
    open class Z(p: Int)
    trait T
    trait U;

    {
        <selection>class A: Z(1), T, U</selection>
    }

    {
        class B: Z(1), U, T
    }

    {
        class C: Z(1)
    }

    {
        class D: Z(1), T
    }

    {
        class E: Z(1), U
    }
}