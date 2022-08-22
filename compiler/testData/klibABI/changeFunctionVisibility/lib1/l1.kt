public fun publicToInternalFunction() = 42
public fun publicToPrivateFunction() = 42

open class Container {
    public fun publicToProtectedFunction() = 42
    public fun publicToInternalFunction() = 42
    public fun publicToPrivateFunction() = 42

    protected fun protectedToPublicFunction() = 42
    protected fun protectedToInternalFunction() = 42
    protected fun protectedToPrivateFunction() = 42
}
