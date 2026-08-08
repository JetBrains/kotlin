// KCallable.returnType is only supported in K/Native
fun removedCtxFunReferenceReturnType(): Any = with("ctx") { with('K') { ::removedCtxFun.returnType } }
fun removedCtxValReferenceReturnType(): Any = with("ctx") { ::removedCtxVal.returnType }
fun removedCtxVarReferenceReturnType(): Any = with("ctx") { ::removedCtxVar.returnType }
private var removedCtxVarDelegate: String by with("ctx") { ::removedCtxVar }
fun removedCtxVarDelegateReferenceReturnType(): Any = ::removedCtxVarDelegate.returnType
fun survivingCtxFunReferenceReturnType(): Any = with("ctx") { with('K') { ::survivingCtxFun.returnType } }
