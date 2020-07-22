// !DIAGNOSTICS: -UNUSED_PARAMETER

class A<T : <!OTHER_ERROR!>Nested<!>, F: <!OTHER_ERROR!>Inner<!>, G: <!OTHER_ERROR!>Interface<!>> {

    class Nested

    inner class Inner

    interface Interface
}

class B<T, F, G> where T : <!OTHER_ERROR!>Nested<!>, F: <!OTHER_ERROR!>Inner<!>, G: <!OTHER_ERROR!>Interface<!> {

    class Nested

    inner class Inner

    interface Interface
}

