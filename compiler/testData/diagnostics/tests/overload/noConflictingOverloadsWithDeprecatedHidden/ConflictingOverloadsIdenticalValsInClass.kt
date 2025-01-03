// RUN_PIPELINE_TILL: BACKEND
// IGNORE_DEXING
class Aaa() {
    val <!REDECLARATION!>a<!> = 1
    @Deprecated("a", level = DeprecationLevel.HIDDEN)
    val <!REDECLARATION!>a<!> = 1
}
