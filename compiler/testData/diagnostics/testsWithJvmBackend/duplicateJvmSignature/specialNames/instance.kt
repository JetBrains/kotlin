// FIR_IDENTICAL

object O {
    val INSTANCE: O = null!!
}

<!CONFLICTING_JVM_DECLARATIONS!>object O2 {
    <!CONFLICTING_JVM_DECLARATIONS!>lateinit var INSTANCE: O2<!>
}<!>
