// FILE: Klass.kt
package pkg

private class Klass

// FILE: test.kt
package pack

import <!UNRESOLVED_REFERENCE!>foo<!>.<!DEBUG_INFO_MISSING_UNRESOLVED!>bar<!>.<!DEBUG_INFO_MISSING_UNRESOLVED!>baz<!>
import <!UNRESOLVED_REFERENCE!>Outer<!>.<!DEBUG_INFO_MISSING_UNRESOLVED!>`<no name provided>`<!>.<!DEBUG_INFO_MISSING_UNRESOLVED!>getInner<!>
import pack.<!UNRESOLVED_REFERENCE!>UnresolvedName<!>
import pkg.<!INVISIBLE_REFERENCE!>Klass<!>

class MainSource
