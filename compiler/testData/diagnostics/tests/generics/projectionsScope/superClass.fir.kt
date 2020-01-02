// !CHECK_TYPE

interface Clazz<T> {
    val t: T
    fun getSuperClass(): Clazz<in T>
}

fun test(clazz: Clazz<*>) {
    clazz.t checkType { <!UNRESOLVED_REFERENCE!>_<!><Any?>() }
    clazz.getSuperClass() checkType { <!UNRESOLVED_REFERENCE!>_<!><Clazz<*>>() }
    clazz.getSuperClass().t checkType { <!UNRESOLVED_REFERENCE!>_<!><Any?>() }
}
