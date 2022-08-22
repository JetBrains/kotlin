class ContainerImpl : Container() {
    // Just to check that accessing from within the class hierarchy has the same effect as accessing from the outside:
    fun publicToProtectedFunctionAccess() = publicToProtectedFunction()
    fun publicToInternalFunctionAccess() = publicToInternalFunction()
    fun publicToPrivateFunctionAccess() = publicToPrivateFunction()

    // As far as protected members can't be accessed outside of the class hierarchy, we need special accessors.
    fun protectedToPublicFunctionAccess() = protectedToPublicFunction()
    fun protectedToInternalFunctionAccess() = protectedToInternalFunction()
    fun protectedToPrivateFunctionAccess() = protectedToPrivateFunction()
}
