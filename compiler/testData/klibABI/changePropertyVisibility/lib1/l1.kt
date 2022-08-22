public val publicToInternalProperty1 = 42
public val publicToInternalProperty2 get() = 42
public val publicToPrivateProperty1 = 42
public val publicToPrivateProperty2 get() = 42

open class Container {
    public val publicToProtectedProperty1 = 42
    public val publicToProtectedProperty2 get() = 42
    public val publicToInternalProperty1 = 42
    public val publicToInternalProperty2 get() = 42
    public val publicToPrivateProperty1 = 42
    public val publicToPrivateProperty2 get() = 42

    protected val protectedToPublicProperty1 = 42
    protected val protectedToPublicProperty2 get() = 42
    protected val protectedToInternalProperty1 = 42
    protected val protectedToInternalProperty2 get() = 42
    protected val protectedToPrivateProperty1 = 42
    protected val protectedToPrivateProperty2 get() = 42
}
