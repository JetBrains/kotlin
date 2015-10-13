class A {

    <!CONFLICTING_JVM_DECLARATIONS!>@JvmField val clash<!> = 1;

    companion object {
        <!CONFLICTING_JVM_DECLARATIONS!>val clash<!> = 1;
    }
}