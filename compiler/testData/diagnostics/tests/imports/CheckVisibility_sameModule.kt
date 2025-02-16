// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL
// MODULE: m1
// FILE: k1.kt
package k

private class KPrivate
internal class KInternal
public class KPublic

class A {
    protected class KProtected
}

// FILE: k2.kt
package k2

import k.<!INVISIBLE_REFERENCE!>KPrivate<!>
import k.KInternal
import k.KPublic
import k.A.KProtected
