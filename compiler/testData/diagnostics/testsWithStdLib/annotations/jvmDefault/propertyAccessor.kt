// FIR_IDENTICAL
// !JVM_TARGET: 1.8
// !JVM_DEFAULT_MODE: enable

interface B {

    @<!DEPRECATION!>JvmDefault<!>
    val prop1: String
    <!WRONG_ANNOTATION_TARGET!>@<!DEPRECATION!>JvmDefault<!><!> get() = ""


    var prop2: String
        <!WRONG_ANNOTATION_TARGET!>@<!DEPRECATION!>JvmDefault<!><!> get() = ""
        <!WRONG_ANNOTATION_TARGET!>@<!DEPRECATION!>JvmDefault<!><!> set(value) {}
}
