// COMPARE_WITH_LIGHT_TREE

interface Base1 {
    fun getX(): Int
}

interface Base2 {
    val x: Int
        get() = 1
}

<!CONFLICTING_INHERITED_JVM_DECLARATIONS{LT}!>interface <!CONFLICTING_INHERITED_JVM_DECLARATIONS{PSI}!>Test<!> : Base1, Base2<!>
