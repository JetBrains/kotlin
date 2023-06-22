// COMPARE_WITH_LIGHT_TREE

class C {
    companion object {
        val X = 1
        <!CONFLICTING_JVM_DECLARATIONS!>val `X$1`<!> = 1
    }

    <!CONFLICTING_JVM_DECLARATIONS!>val X<!> = 1
}
