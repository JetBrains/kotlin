//FILE:a.kt
//KT-1080 Don't use previously imported packages while resolving import references

package kt1080

import <!UNRESOLVED_REFERENCE!>reflect<!>.Constructor

import b.*
import <!UNRESOLVED_REFERENCE!>d<!>
import b.d


//FILE:b.kt

package b.d
