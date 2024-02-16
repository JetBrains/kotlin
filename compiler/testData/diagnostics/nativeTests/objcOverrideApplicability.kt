// !API_VERSION: 2.0
// FIR_IDENTICAL
// WITH_PLATFORM_LIBS

import kotlinx.cinterop.*
import platform.darwin.*

<!INAPPLICABLE_OBJC_OVERRIDE!>@ObjCSignatureOverride<!>
fun foo() = 1

class A {
    <!INAPPLICABLE_OBJC_OVERRIDE!>@ObjCSignatureOverride<!>
    fun foo() = 1
}

<!WRONG_ANNOTATION_TARGET!>@ObjCSignatureOverride<!>
class B : NSObject() {
    <!INAPPLICABLE_OBJC_OVERRIDE!>@ObjCSignatureOverride<!>
    fun foo() = 1
    <!WRONG_ANNOTATION_TARGET!>@ObjCSignatureOverride<!>
    val v = 1
}
