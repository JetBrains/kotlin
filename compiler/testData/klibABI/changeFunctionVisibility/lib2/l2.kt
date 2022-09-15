class ContainerImpl : Container() {
    // Just to check that accessing from within the class hierarchy has the same effect as accessing from the outside:
    fun publicToProtectedFunctionAccess() = publicToProtectedFunction()
    fun publicToInternalFunctionAccess() = publicToInternalFunction()
    fun publicToPrivateFunctionAccess() = publicToPrivateFunction()

    // As far as protected members can't be accessed outside of the class hierarchy, we need special accessors.
    fun protectedToPublicFunctionAccess() = protectedToPublicFunction()
    fun protectedToInternalFunctionAccess() = protectedToInternalFunction()
    fun protectedToPrivateFunctionAccess() = protectedToPrivateFunction()

    // Overridden functions with changed visibility:
    override fun publicToProtectedOverriddenFunction() = "ContainerImpl.publicToProtectedOverriddenFunction"
    override fun publicToInternalOverriddenFunction() = "ContainerImpl.publicToInternalOverriddenFunction"
    override fun publicToPrivateOverriddenFunction() = "ContainerImpl.publicToPrivateOverriddenFunction"

    override fun protectedToPublicOverriddenFunction() = "ContainerImpl.protectedToPublicOverriddenFunction"
    override fun protectedToInternalOverriddenFunction() = "ContainerImpl.protectedToInternalOverriddenFunction"
    override fun protectedToPrivateOverriddenFunction() = "ContainerImpl.protectedToPrivateOverriddenFunction"

    // As far as protected members can't be accessed outside of the class hierarchy, we need special accessors.
    fun protectedToPublicOverriddenFunctionAccess() = protectedToPublicOverriddenFunction()
    fun protectedToInternalOverriddenFunctionAccess() = protectedToInternalOverriddenFunction()
    fun protectedToPrivateOverriddenFunctionAccess() = protectedToPrivateOverriddenFunction()
}
