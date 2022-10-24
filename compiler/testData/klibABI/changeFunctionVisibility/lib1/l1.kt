public fun publicToInternalTopLevelFunction() = "publicToInternalTopLevelFunction.v1"
public fun publicToPrivateTopLevelFunction() = "publicToPrivateTopLevelFunction.v1"

open class Container {
    public fun publicToProtectedFunction() = "Container.publicToProtectedFunction.v1"
    public fun publicToInternalFunction() = "Container.publicToInternalFunction.v1"
    public fun publicToPrivateFunction() = "Container.publicToPrivateFunction.v1"

    protected fun protectedToPublicFunction() = "Container.protectedToPublicFunction.v1"
    protected fun protectedToInternalFunction() = "Container.protectedToInternalFunction.v1"
    protected fun protectedToPrivateFunction() = "Container.protectedToPrivateFunction.v1"

    public open fun publicToProtectedOverriddenFunction() = "Container.publicToProtectedOverriddenFunction.v1"
    public open fun publicToInternalOverriddenFunction() = "Container.publicToInternalOverriddenFunction.v1"
    public open fun publicToPrivateOverriddenFunction() = "Container.publicToPrivateOverriddenFunction.v1"

    protected open fun protectedToPublicOverriddenFunction() = "Container.protectedToPublicOverriddenFunction.v1"
    protected open fun protectedToInternalOverriddenFunction() = "Container.protectedToInternalOverriddenFunction.v1"
    protected open fun protectedToPrivateOverriddenFunction() = "Container.protectedToPrivateOverriddenFunction.v1"

//    public fun newPublicFunction() = "Container.newPublicFunction.v1"
//    public open fun newOpenPublicFunction() = "Container.newOpenPublicFunction.v1"
//    protected fun newProtectedFunction() = "Container.newProtectedFunction.v1"
//    protected open fun newOpenProtectedFunction() = "Container.newOpenProtectedFunction.v1"
//    internal fun newInternalFunction() = "Container.newInternalFunction.v1"
//    internal open fun newOpenInternalFunction() = "Container.newOpenInternalFunction.v1"
//    private fun newPrivateFunction() = "Container.newPrivateFunction.v1"
}
