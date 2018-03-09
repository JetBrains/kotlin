// !DIAGNOSTICS: -EXPERIMENTAL_API_USAGE
// !API_VERSION: 1.3
// !JVM_TARGET: 1.8
interface B {

    @JvmDefault
    val prop1: String
    <!WRONG_ANNOTATION_TARGET!>@JvmDefault<!> get() = ""


    var prop2: String
        <!WRONG_ANNOTATION_TARGET!>@JvmDefault<!> get() = ""
        <!WRONG_ANNOTATION_TARGET!>@JvmDefault<!> set(value) {}
}