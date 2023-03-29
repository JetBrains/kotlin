// FILE: Klass.kt
package pkg

private class Klass

private class Foo {
    object Bar {
        fun baz() {}
    }
}

class Foo2 {
    private object Bar {
        fun baz() {}
    }
}

private class Foo3 {
    private object Bar {
        private fun baz() {}
    }
}

// FILE: test.kt
package pack

import <!UNRESOLVED_IMPORT!>foo<!>.bar.baz
import <!UNRESOLVED_IMPORT!>Outer<!>.`<no name provided>`.getInner
import pack.<!UNRESOLVED_IMPORT!>UnresolvedName<!>
import pkg.<!INVISIBLE_REFERENCE!>Klass<!>
import pkg.<!INVISIBLE_REFERENCE!>Foo<!>.Bar.baz
import pkg.Foo2.<!INVISIBLE_REFERENCE!>Bar<!>.baz
import pkg.<!INVISIBLE_REFERENCE!>Foo3<!>.<!INVISIBLE_REFERENCE!>Bar<!>.<!INVISIBLE_REFERENCE!>baz<!>

class MainSource
