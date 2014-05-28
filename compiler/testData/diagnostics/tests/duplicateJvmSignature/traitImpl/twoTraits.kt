trait T1 {
    fun getX() = 1
}

trait T2 {
    val x: Int
        get() = 1
}

<!CONFLICTING_JVM_DECLARATIONS!>class C : T1, T2<!> {
}