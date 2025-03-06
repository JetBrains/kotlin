// fun
fun createRemovedFunReference(): Any = ::removedFun
fun removedFunReferenceName(): String = ::removedFun.name
fun removedFunReferenceReturnType(): Any = ::removedFun.returnType
fun removedFunReferenceHashCode(): Int = ::removedFun.hashCode()
fun removedFunReferenceEquals(): Boolean = ::removedFun.equals(Any())
fun removedFunReferenceToString(): String = ::removedFun.toString()
fun removedFunReferenceInvoke(): Int = ::removedFun.invoke(123)

// inline fun
fun createRemovedInlineFunReference(): Any = ::removedInlineFun
fun removedInlineFunReferenceName(): String = ::removedInlineFun.name
fun removedInlineFunReferenceReturnType(): Any = ::removedInlineFun.returnType
fun removedInlineFunReferenceHashCode(): Int = ::removedInlineFun.hashCode()
fun removedInlineFunReferenceEquals(): Boolean = ::removedInlineFun.equals(Any())
fun removedInlineFunReferenceToString(): String = ::removedInlineFun.toString()
fun removedInlineFunReferenceInvoke(): Int = ::removedInlineFun.invoke(123)

// constructor
fun createRemovedCtorReference(): Any = ::ClassWithRemovedCtor
fun removedCtorReferenceName(): String = ::ClassWithRemovedCtor.name
fun removedCtorReferenceReturnType(): Any = ::ClassWithRemovedCtor.returnType
fun removedCtorReferenceHashCode(): Int = ::ClassWithRemovedCtor.hashCode()
fun removedCtorReferenceEquals(): Boolean = ::ClassWithRemovedCtor.equals(Any())
fun removedCtorReferenceToString(): String = ::ClassWithRemovedCtor.toString()
fun removedCtorReferenceInvoke(): ClassWithRemovedCtor = ::ClassWithRemovedCtor.invoke(123)

fun funReferenceWithErrorInReceiver(): Any = removedGetRegularClassInstance()::foo

// val
fun createRemovedValReference(): Any = ::removedVal
fun removedValReferenceName(): String = ::removedVal.name
fun removedValReferenceReturnType(): Any = ::removedVal.returnType
fun removedValReferenceHashCode(): Int = ::removedVal.hashCode()
fun removedValReferenceEquals(): Boolean = ::removedVal.equals(Any())
fun removedValReferenceToString(): String = ::removedVal.toString()
fun removedValReferenceInvoke(): Int = ::removedVal.invoke()
fun removedValReferenceGet(): Int = ::removedVal.get()

// inline val
fun createRemovedInlineValReference(): Any = ::removedInlineVal
fun removedInlineValReferenceName(): String = ::removedInlineVal.name
fun removedInlineValReferenceReturnType(): Any = ::removedInlineVal.returnType
fun removedInlineValReferenceHashCode(): Int = ::removedInlineVal.hashCode()
fun removedInlineValReferenceEquals(): Boolean = ::removedInlineVal.equals(Any())
fun removedInlineValReferenceToString(): String = ::removedInlineVal.toString()
fun removedInlineValReferenceInvoke(): Int = ::removedInlineVal.invoke()
fun removedInlineValReferenceGet(): Int = ::removedInlineVal.get()

// var
fun createRemovedVarReference(): Any = ::removedVar
fun removedVarReferenceName(): String = ::removedVar.name
fun removedVarReferenceReturnType(): Any = ::removedVar.returnType
fun removedVarReferenceHashCode(): Int = ::removedVar.hashCode()
fun removedVarReferenceEquals(): Boolean = ::removedVar.equals(Any())
fun removedVarReferenceToString(): String = ::removedVar.toString()
fun removedVarReferenceInvoke(): Int = ::removedVar.invoke()
fun removedVarReferenceGet(): Int = ::removedVar.get()
fun removedVarReferenceSet(): Unit = ::removedVar.set(123)

// inline var
fun createRemovedInlineVarReference(): Any = ::removedInlineVar
fun removedInlineVarReferenceName(): String = ::removedInlineVar.name
fun removedInlineVarReferenceReturnType(): Any = ::removedInlineVar.returnType
fun removedInlineVarReferenceHashCode(): Int = ::removedInlineVar.hashCode()
fun removedInlineVarReferenceEquals(): Boolean = ::removedInlineVar.equals(Any())
fun removedInlineVarReferenceToString(): String = ::removedInlineVar.toString()
fun removedInlineVarReferenceInvoke(): Int = ::removedInlineVar.invoke()
fun removedInlineVarReferenceGet(): Int = ::removedInlineVar.get()
fun removedInlineVarReferenceSet(): Unit = ::removedInlineVar.set(123)

// var by ::var
private var removedVarDelegate: Int by ::removedVar
fun createRemovedVarDelegateReference(): Any = ::removedVarDelegate
fun removedVarDelegateReferenceName(): String = ::removedVarDelegate.name
fun removedVarDelegateReferenceReturnType(): Any = ::removedVarDelegate.returnType
fun removedVarDelegateReferenceHashCode(): Int = ::removedVarDelegate.hashCode()
fun removedVarDelegateReferenceEquals(): Boolean = ::removedVarDelegate.equals(Any())
fun removedVarDelegateReferenceToString(): String = ::removedVarDelegate.toString()
fun removedVarDelegateReferenceInvoke(): Int = ::removedVarDelegate.invoke()
fun removedVarDelegateReferenceGet(): Int = ::removedVarDelegate.get()
fun removedVarDelegateReferenceSet(): Unit = ::removedVarDelegate.set(123)