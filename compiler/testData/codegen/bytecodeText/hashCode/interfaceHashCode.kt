val x: () -> Unit = {}
val y = x.hashCode()

// 1 INVOKEVIRTUAL java/lang/Object.hashCode \(\)I
// 0 INVOKEINTERFACE
