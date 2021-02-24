interface ResolvedCall<D : CallableDescriptor> {
    var d: D
}

interface CallableDescriptor

fun <D : CallableDescriptor> ResolvedCall<D>.getParameterForArgument(): String = ""

fun <D : CallableDescriptor> ResolvedCall<D>.updateD(d: D): D {
    this.d = d
    return d
}

fun test_1_1(resolvedCall: ResolvedCall<out CallableDescriptor>) {
    resolvedCall.getParameterForArgument() // should be ok
}

fun test_1_2(resolvedCall: ResolvedCall<in CallableDescriptor>) {
    resolvedCall.getParameterForArgument() // should be ok
}

fun test_1_3(resolvedCall: ResolvedCall<CallableDescriptor>) {
    resolvedCall.getParameterForArgument() // should be ok
}

fun test_2_1(resolvedCall: ResolvedCall<out CallableDescriptor>, d: CallableDescriptor) {
    val x = resolvedCall.<!INAPPLICABLE_CANDIDATE!>updateD<!>(d) // should fail
}

fun test_2_2(resolvedCall: ResolvedCall<in CallableDescriptor>, d: CallableDescriptor) {
    val x = resolvedCall.updateD(d) // should be ok
}

fun test_2_3(resolvedCall: ResolvedCall<CallableDescriptor>, d: CallableDescriptor) {
    val x = resolvedCall.updateD(d) // should be ok
}
