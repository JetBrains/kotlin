class A(val x: String) {
    fun y() = x
    // Both `x` and `y()` assumed to respect their nullability information,
    // so only `a` and `b` need to be checked.
    fun foo1(a: A?, b: A?) = a?.x ?: b?.x ?: x       // if (a == null) if (b == null) x else b.x else a.x
    fun foo2(a: A?, b: A?) = a?.y() ?: b?.y() ?: y() // if (a == null) if (b == null) y() else b.y() else a.y()
}

// JVM_TEMPLATES
// Optimization not implemented
// 4 IFNULL
// 4 IFNONNULL
// 2 ACONST_NULL

// JVM_IR_TEMPLATES
// 4 IFNULL
// 2 IFNONNULL
// 0 ACONST_NULL
