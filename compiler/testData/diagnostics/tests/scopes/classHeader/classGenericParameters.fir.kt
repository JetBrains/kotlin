// !DIAGNOSTICS: -UNUSED_PARAMETER

class A<T : Nested, F: Inner, G: Interface> {

    class Nested

    inner class Inner

    interface Interface
}

class B<T, F, G> where T : Nested, F: Inner, G: Interface {

    class Nested

    inner class Inner

    interface Interface
}

