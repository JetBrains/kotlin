// FIR_COMPARISON
val String.extensionProp1: Int get() = 1
val String.extensionProp2: Int get() = 1

fun foo(o: Any) {
    if (o !is String) return
    o.ext<caret>
}

// EXIST: extensionProp1
// EXIST: extensionProp2
// EXIST: extensionPropNotImported
