fun foo() {
    <selection>{
        open class Z<T>(p: T)
        interface T
        class A: Z<Int>(1), T
    }</selection>

    {
        open class Z<A>(r: A)
        interface T
        class B: Z<Int>(1), T
    }

    {
        open class T<A>(r: A)
        interface Z
        class C: T<Int>(1), Z
    }

    {
        open class Z<T>(q: T)
        interface T
        class D: Z<Int>(2), T
    }

    {
        open class Z<T>(q: T)
        class E: Z<Int>(1)
    }

    {
        open class Z<A>(r: A)
        interface T
        class F: Z<String>("1"), T
    }

    {
        open class Z(r: Int)
        interface T
        class B: Z(1), T
    }
}