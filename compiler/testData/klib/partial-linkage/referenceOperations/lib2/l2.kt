// fun
fun createRemovedFunReference(): Any = ::removedFun
fun removedFunReferenceName(): String = ::removedFun.name
fun removedFunReferenceHashCode(): Int = ::removedFun.hashCode()
fun removedFunReferenceEquals(): Boolean = ::removedFun.equals(Any())
fun removedFunReferenceToString(): String = ::removedFun.toString()
fun removedFunReferenceInvoke(): Int = ::removedFun.invoke(123)

// constructor
fun createRemovedCtorReference(): Any = ::ClassWithRemovedCtor
fun removedCtorReferenceName(): String = ::ClassWithRemovedCtor.name
fun removedCtorReferenceHashCode(): Int = ::ClassWithRemovedCtor.hashCode()
fun removedCtorReferenceEquals(): Boolean = ::ClassWithRemovedCtor.equals(Any())
fun removedCtorReferenceToString(): String = ::ClassWithRemovedCtor.toString()
fun removedCtorReferenceInvoke(): ClassWithRemovedCtor = ::ClassWithRemovedCtor.invoke(123)

fun funReferenceWithErrorInReceiver(): Any = removedGetRegularClassInstance()::foo

// val
fun createRemovedValReference(): Any = ::removedVal
fun removedValReferenceName(): String = ::removedVal.name
fun removedValReferenceHashCode(): Int = ::removedVal.hashCode()
fun removedValReferenceEquals(): Boolean = ::removedVal.equals(Any())
fun removedValReferenceToString(): String = ::removedVal.toString()
fun removedValReferenceInvoke(): Int = ::removedVal.invoke()
fun removedValReferenceGet(): Int = ::removedVal.get()

// var
fun createRemovedVarReference(): Any = ::removedVar
fun removedVarReferenceName(): String = ::removedVar.name
fun removedVarReferenceHashCode(): Int = ::removedVar.hashCode()
fun removedVarReferenceEquals(): Boolean = ::removedVar.equals(Any())
fun removedVarReferenceToString(): String = ::removedVar.toString()
fun removedVarReferenceInvoke(): Int = ::removedVar.invoke()
fun removedVarReferenceGet(): Int = ::removedVar.get()
fun removedVarReferenceSet(): Unit = ::removedVar.set(123)

// var by ::var
private var removedVarDelegate: Int by ::removedVar
fun createRemovedVarDelegateReference(): Any = ::removedVarDelegate
fun removedVarDelegateReferenceName(): String = ::removedVarDelegate.name
fun removedVarDelegateReferenceHashCode(): Int = ::removedVarDelegate.hashCode()
fun removedVarDelegateReferenceEquals(): Boolean = ::removedVarDelegate.equals(Any())
fun removedVarDelegateReferenceToString(): String = ::removedVarDelegate.toString()
fun removedVarDelegateReferenceInvoke(): Int = ::removedVarDelegate.invoke()
fun removedVarDelegateReferenceGet(): Int = ::removedVarDelegate.get()
fun removedVarDelegateReferenceSet(): Unit = ::removedVarDelegate.set(123)