public val publicToInternalTopLevelProperty1 = "publicToInternalTopLevelProperty1.v1"
public val publicToInternalTopLevelProperty2 get() = "publicToInternalTopLevelProperty2.v1"
public val publicToPrivateTopLevelProperty1 = "publicToPrivateTopLevelProperty1.v1"
public val publicToPrivateTopLevelProperty2 get() = "publicToPrivateTopLevelProperty2.v1"

open class Container {
    public val publicToProtectedProperty1 = "Container.publicToProtectedProperty1.v1"
    public val publicToProtectedProperty2 get() = "Container.publicToProtectedProperty2.v1"
    public val publicToInternalProperty1 = "Container.publicToInternalProperty1.v1"
    public val publicToInternalProperty2 get() = "Container.publicToInternalProperty2.v1"
    public val publicToPrivateProperty1 = "Container.publicToPrivateProperty1.v1"
    public val publicToPrivateProperty2 get() = "Container.publicToPrivateProperty2.v1"

    protected val protectedToPublicProperty1 = "Container.protectedToPublicProperty1.v1"
    protected val protectedToPublicProperty2 get() = "Container.protectedToPublicProperty2.v1"
    protected val protectedToInternalProperty1 = "Container.protectedToInternalProperty1.v1"
    protected val protectedToInternalProperty2 get() = "Container.protectedToInternalProperty2.v1"
    protected val protectedToPrivateProperty1 = "Container.protectedToPrivateProperty1.v1"
    protected val protectedToPrivateProperty2 get() = "Container.protectedToPrivateProperty2.v1"
}
