// COMPARE_WITH_LIGHT_TREE

interface T1 {
    fun getX() = 1
}

interface T2 {
    val x: Int
        get() = 1
}

<!CONFLICTING_INHERITED_JVM_DECLARATIONS{LT}!>class <!CONFLICTING_INHERITED_JVM_DECLARATIONS{PSI}!>C<!> : T1, T2 {
}<!>
