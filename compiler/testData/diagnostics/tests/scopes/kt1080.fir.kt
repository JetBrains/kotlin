//FILE:a.kt
//KT-1080 Don't use previously imported packages while resolving import references

package kt1080

import <!UNRESOLVED_IMPORT!>reflect<!>.Constructor

import b.*
import <!UNRESOLVED_IMPORT!>d<!>
import <!UNRESOLVED_IMPORT!>d<!>.Test
import b.<!PACKAGE_CANNOT_BE_IMPORTED!>d<!>

class Some: <!UNRESOLVED_REFERENCE!>Test<!>()

//FILE:b.kt

package b.d

public open class Test
