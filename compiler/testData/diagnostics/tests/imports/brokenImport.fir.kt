// FILE: Klass.kt
package pkg

private class Klass

// FILE: test.kt
package pack

import <!UNRESOLVED_IMPORT!>foo<!>.bar.baz
import <!UNRESOLVED_IMPORT!>Outer<!>.`<no name provided>`.getInner
import pack.<!UNRESOLVED_IMPORT!>UnresolvedName<!>
import pkg.Klass

class MainSource
