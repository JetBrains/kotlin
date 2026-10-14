private fun resolveAccessorCall(
    suspendPropertyDescriptor: PropertyDescriptor,
    context: TranslationContext
): ResolvedCall<PropertyDescriptor> {
    return object : ResolvedCall<PropertyDescriptor> {
        override fun getStatus() = ResolutionStatus.SUCCESS

        override fun getCandidateDescriptor() = suspendPropertyDescriptor
        override fun getResultingDescriptor() = suspendPropertyDescriptor
    }
}