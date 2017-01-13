fun B<(@A C)>.receiverArgument()
fun B<(@A C)?>.receiverArgumentN()

fun parameter(a: (@A C))
fun parameterN(a: (@A C)?)

fun parameterArgument(a: B<(@A C)>)
fun parameterArgumentN(a: B<(@A C)?>)

fun returnValue(): (@A C)
fun returnValueN(): (@A C)?

fun <T> returnTypeParameterValue(): (@A T)
fun <T> returnTypeParameterValueN(): (@A T)?

fun returnArgument(): B<(@A C)>
fun returnArgumentN(): B<(@A C)>?

val lambdaType: (@A() (() -> C))
val lambdaTypeN: (@A() (() -> C))?

val lambdaParameter: ((@A C)) -> C
val lambdaParameterN: ((@A C))? -> C

val lambdaReturnValue: () -> (@A C)
val lambdaReturnValueN: () -> (@A C)?

val lambdaReceiver: (@A C).() -> C
val lambdaReceiverN: (@A C)?.() -> C

val suspendT: suspend T
val suspendTN: suspend T?

val suspendFun: suspend () -> Unit
val suspendFunN: (suspend () -> Unit)?

val suspendExtFun: suspend Any.() -> Unit
val suspendExtFunN: (suspend Any.() -> Unit)?

val suspendFunReturnValueN: suspend () -> Unit?
val suspendFunNReturnValueN: (suspend () -> Unit?)?

val suspendExtFunReceiverN: suspend Any?.() -> Unit
val suspendExtFunNReceiverN: (suspend Any?.() -> Unit)?

val suspendFunReturnValueN: suspend () -> Unit?
val suspendFunNReturnValueN: (suspend () -> Unit?)?

val suspendExtFunReceiverN: suspend Any?.() -> Unit
val suspendExtFunNReceiverN: (suspend Any?.() -> Unit)?
