// TARGET_BACKEND: JVM_OLD
interface Base1 {
    fun getX(): Int
}

interface Base2 {
    val x: Int
        get() = 1
}

interface <!CONFLICTING_INHERITED_JVM_DECLARATIONS, CONFLICTING_INHERITED_JVM_DECLARATIONS!>Test<!> : Base1, Base2