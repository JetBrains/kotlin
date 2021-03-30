// FIR_IGNORE
private fun resolveAccessorCall(
//                             [ERROR : PropertyDescriptor]
//                             │
    suspendPropertyDescriptor: PropertyDescriptor,
//           [ERROR : TranslationContext]
//           │
    context: TranslationContext
// [ERROR : ResolvedCall<PropertyDescriptor>]<[ERROR : PropertyDescriptor]>
// │
): ResolvedCall<PropertyDescriptor> {
//                  [ERROR : ResolvedCall<PropertyDescriptor>]<[ERROR : PropertyDescriptor]>
//                  │
    return object : ResolvedCall<PropertyDescriptor> {
//                               [ERROR : <ERROR PROPERTY TYPE>]
//                               │ [ERROR: not resolved]
//                               │ │                [ERROR: not resolved]
//                               │ │                │
        override fun getStatus() = ResolutionStatus.SUCCESS

//                                            [ERROR : PropertyDescriptor]
//                                            │ resolveAccessorCall.suspendPropertyDescriptor: [ERROR : PropertyDescriptor]
//                                            │ │
        override fun getCandidateDescriptor() = suspendPropertyDescriptor
//                                            [ERROR : PropertyDescriptor]
//                                            │ resolveAccessorCall.suspendPropertyDescriptor: [ERROR : PropertyDescriptor]
//                                            │ │
        override fun getResultingDescriptor() = suspendPropertyDescriptor
    }
}
