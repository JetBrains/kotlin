// direct access
fun getRemovedCompanion(): Any = B.Companion
fun removedCompanionValCall(): Int = B.removedCompanionVal
fun removedCompanionVarCall(): Int = B.removedCompanionVar
fun removedCompanionVarSetCall() {
    B.removedCompanionVar = 0
}
fun removedCompanionFunCall(): String = B.removedCompanionFun()

// fun
fun createRemovedCompanionFunRef(): Any = B::removedCompanionFun
fun removedCompanionFunRefName(): String = B::removedCompanionFun.name
fun removedCompanionFunRefInvoke(): String = B::removedCompanionFun.invoke()

// val
fun createRemovedCompanionValRef(): Any = B::removedCompanionVal
fun removedCompanionValRefName(): String = B::removedCompanionVal.name
fun removedCompanionValRefInvoke(): Int = B::removedCompanionVal.invoke()
fun removedCompanionValRefGet(): Int = B::removedCompanionVal.get()

// var
fun createRemovedCompanionVarRef(): Any = B::removedCompanionVar
fun removedCompanionVarRefName(): String = B::removedCompanionVar.name
fun removedCompanionVarRefInvoke(): Int = B::removedCompanionVar.invoke()
fun removedCompanionVarRefGet(): Int = B::removedCompanionVar.get()
fun removedCompanionVarRefSet(): Unit = B::removedCompanionVar.set(123)
