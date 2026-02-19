// KCallable.returnType is only supported in K/Native
fun removedFunReferenceReturnType(): Any = ::removedFun.returnType
fun removedCtorReferenceReturnType(): Any = ::ClassWithRemovedCtor.returnType
fun removedValReferenceReturnType(): Any = ::removedVal.returnType
fun removedVarReferenceReturnType(): Any = ::removedVar.returnType
private var removedVarDelegate: Int by ::removedVar
fun removedVarDelegateReferenceReturnType(): Any = ::removedVarDelegate.returnType
