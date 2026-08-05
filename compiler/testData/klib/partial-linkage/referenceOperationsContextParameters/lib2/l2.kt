// fun
fun createRemovedCtxFunReference(): Any = with("ctx") { with('K') { ::removedCtxFun } }
fun removedCtxFunReferenceName(): String = with("ctx") { with('K') { ::removedCtxFun.name } }
fun removedCtxFunReferenceHashCode(): Int = with("ctx") { with('K') { ::removedCtxFun.hashCode() } }
fun removedCtxFunReferenceEquals(): Boolean = with("ctx") { with('K') { ::removedCtxFun.equals(Any()) } }
fun removedCtxFunReferenceToString(): String = with("ctx") { with('K') { ::removedCtxFun.toString() } }
fun removedCtxFunReferenceInvoke(): String = with("ctx") { with('K') { ::removedCtxFun.invoke(123) } }

// member fun
fun removedMemberCtxFunReferenceName(): String = with("ctx") { ObjectWithRemovedCtxFun::removedMemberCtxFun.name }
fun removedMemberCtxFunReferenceInvoke(): String = with("ctx") { ObjectWithRemovedCtxFun::removedMemberCtxFun.invoke(123) }

// val
fun createRemovedCtxValReference(): Any = with("ctx") { ::removedCtxVal }
fun removedCtxValReferenceName(): String = with("ctx") { ::removedCtxVal.name }
fun removedCtxValReferenceHashCode(): Int = with("ctx") { ::removedCtxVal.hashCode() }
fun removedCtxValReferenceEquals(): Boolean = with("ctx") { ::removedCtxVal.equals(Any()) }
fun removedCtxValReferenceToString(): String = with("ctx") { ::removedCtxVal.toString() }
fun removedCtxValReferenceInvoke(): String = with("ctx") { ::removedCtxVal.invoke() }
fun removedCtxValReferenceGet(): String = with("ctx") { ::removedCtxVal.get() }

// var
fun createRemovedCtxVarReference(): Any = with("ctx") { ::removedCtxVar }
fun removedCtxVarReferenceName(): String = with("ctx") { ::removedCtxVar.name }
fun removedCtxVarReferenceHashCode(): Int = with("ctx") { ::removedCtxVar.hashCode() }
fun removedCtxVarReferenceEquals(): Boolean = with("ctx") { ::removedCtxVar.equals(Any()) }
fun removedCtxVarReferenceToString(): String = with("ctx") { ::removedCtxVar.toString() }
fun removedCtxVarReferenceInvoke(): String = with("ctx") { ::removedCtxVar.invoke() }
fun removedCtxVarReferenceGet(): String = with("ctx") { ::removedCtxVar.get() }
fun removedCtxVarReferenceSet(): Unit = with("ctx") { ::removedCtxVar.set("value") }

// var delegated to a reference to the var with context parameter. Unlike the referenced var,
// the delegating property is declared in this library and survives, so only get/set fail
private var removedCtxVarDelegate: String by with("ctx") { ::removedCtxVar }
fun createRemovedCtxVarDelegateReference(): Any = ::removedCtxVarDelegate
fun removedCtxVarDelegateReferenceName(): String = ::removedCtxVarDelegate.name
fun removedCtxVarDelegateReferenceHashCode(): Int = ::removedCtxVarDelegate.hashCode()
fun removedCtxVarDelegateReferenceEquals(): Boolean = ::removedCtxVarDelegate.equals(Any())
fun removedCtxVarDelegateReferenceToString(): String = ::removedCtxVarDelegate.toString()
fun removedCtxVarDelegateReferenceInvoke(): String = ::removedCtxVarDelegate.invoke()
fun removedCtxVarDelegateReferenceGet(): String = ::removedCtxVarDelegate.get()
fun removedCtxVarDelegateReferenceSet(): Unit = ::removedCtxVarDelegate.set("value")

// changed context parameters
fun funWithAddedCtxReferenceName(): String = with("ctx") { ::funWithAddedCtx.name }
fun funWithAddedCtxReferenceInvoke(): String = with("ctx") { ::funWithAddedCtx.invoke(123) }

fun funWithRemovedCtxReferenceName(): String = with("ctx") { with('K') { ::funWithRemovedCtx.name } }
fun funWithRemovedCtxReferenceInvoke(): String = with("ctx") { with('K') { ::funWithRemovedCtx.invoke(123) } }

fun funWithCtxTurnedIntoParamReferenceInvoke(): String = with("ctx") { ::funWithCtxTurnedIntoParam.invoke(123) }

fun funWithParamTurnedIntoCtxReferenceInvoke(): String = ::funWithParamTurnedIntoCtx.invoke("ctx", 123)

fun funWithAllCtxRemovedReferenceName(): String = with("ctx") { with('K') { ::funWithAllCtxRemoved.name } }
fun funWithAllCtxRemovedReferenceInvoke(): String = with("ctx") { with('K') { ::funWithAllCtxRemoved.invoke(123) } }

fun funWithCtxGainedReferenceName(): String = ::funWithCtxGained.name
fun funWithCtxGainedReferenceInvoke(): String = ::funWithCtxGained.invoke(123)

fun funWithSwappedCtxReferenceInvoke(): String = with("ctx") { with('K') { ::funWithSwappedCtx.invoke(123) } }

fun extFunMigratedToCtxReferenceInvoke(): String = "recv"::extFunMigratedToCtx.invoke(123)

fun funWithRenamedCtxReferenceName(): String = with("ctx") { ::funWithRenamedCtx.name }
fun funWithRenamedCtxReferenceInvoke(): String = with("ctx") { ::funWithRenamedCtx.invoke(123) }

fun valWithChangedCtxTypeReferenceName(): String = with("ctx") { ::valWithChangedCtxType.name }
fun valWithChangedCtxTypeReferenceGet(): String = with("ctx") { ::valWithChangedCtxType.get() }

// extension fun
fun removedCtxExtFunReferenceName(): String = with("ctx") { 5::removedCtxExtFun.name }
fun removedCtxExtFunReferenceInvoke(): String = with("ctx") { 5::removedCtxExtFun.invoke(123) }

// unbound extension fun
fun unboundRemovedCtxExtFunReferenceName(): String = with("ctx") { Int::removedCtxExtFun.name }
fun unboundRemovedCtxExtFunReferenceInvoke(): String = with("ctx") { Int::removedCtxExtFun.invoke(5, 123) }

// removed context parameter type: the context argument expression fails before the reference is created
context(c: RemovedCtxClass?)
fun funWithUnlinkedCtxParameter(x: Int): String = "funWithUnlinkedCtxParameter($x)"

fun createFunWithUnlinkedCtxParameterReference(): Any = with(null as RemovedCtxClass?) { ::funWithUnlinkedCtxParameter }

// surviving references
fun survivingCtxFunReferenceName(): String = with("ctx") { with('K') { ::survivingCtxFun.name } }
fun survivingCtxFunReferenceInvoke(): String = with("ctx") { with('K') { ::survivingCtxFun.invoke(123) } }
fun survivingMemberCtxFunReferenceInvoke(): String = with("ctx") { ObjectWithSurvivingCtxFun::survivingMemberCtxFun.invoke(123) }
fun survivingCtxExtFunReferenceInvoke(): String = with("ctx") { 5::survivingCtxExtFun.invoke(123) }
fun unboundSurvivingCtxExtFunReferenceInvoke(): String = with("ctx") { Int::survivingCtxExtFun.invoke(5, 123) }
fun survivingCtxVarReferenceSetAndGet(): String = with("ctx") {
    val reference = ::survivingCtxVar
    reference.set("value")
    reference.get()
}
