// !JVM_TARGET: 1.8
// !JVM_DEFAULT_MODE: enable

interface B {

    @JvmDefault
    val prop1: String
    <!WRONG_ANNOTATION_TARGET!>@JvmDefault<!> get() = ""


    var prop2: String
        <!WRONG_ANNOTATION_TARGET!>@JvmDefault<!> get() = ""
        <!WRONG_ANNOTATION_TARGET!>@JvmDefault<!> set(<!UNUSED_PARAMETER!>value<!>) {}
}
