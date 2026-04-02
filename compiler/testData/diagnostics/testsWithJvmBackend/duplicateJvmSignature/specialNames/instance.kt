object O {
    val INSTANCE: O = null!!
}

<!CONFLICTING_JVM_DECLARATIONS!>object O2 {
    lateinit <!CONFLICTING_JVM_DECLARATIONS!>var INSTANCE: O2<!>
}<!>
