val recursive1 = recursive2
val recursive2 = recursive1
fun selfRecursive() = selfRecursive()
fun unresolvedCall() = unresolved()
val unresolvedType: UnresolvedType = TODO()
val getterWithBlockBody get() { return 1 }
