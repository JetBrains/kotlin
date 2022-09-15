public fun publicToInternalTopLevelFunction() = "publicToInternalTopLevelFunction.v1"
public fun publicToPrivateTopLevelFunction() = "publicToPrivateTopLevelFunction.v1"

open class Container {
    public fun publicToProtectedFunction() = "Container.publicToProtectedFunction.v1"
    public fun publicToInternalFunction() = "Container.publicToInternalFunction.v1"
    public fun publicToPrivateFunction() = "Container.publicToPrivateFunction.v1"

    protected fun protectedToPublicFunction() = "Container.protectedToPublicFunction.v1"
    protected fun protectedToInternalFunction() = "Container.protectedToInternalFunction.v1"
    protected fun protectedToPrivateFunction() = "Container.protectedToPrivateFunction.v1"
}
