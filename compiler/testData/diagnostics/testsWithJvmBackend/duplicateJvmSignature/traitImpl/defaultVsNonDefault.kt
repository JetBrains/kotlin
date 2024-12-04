// FIR_IDENTICAL

interface Base1 {
    fun getX(): Int
}

interface Base2 {
    val x: Int
        get() = 1
}

<!CONFLICTING_INHERITED_JVM_DECLARATIONS!>interface Test : Base1, Base2<!>
