class ContainerImpl : Container() {
    // Just to check that accessing from within the class hierarchy has the same effect as accessing from the outside:
    fun publicToProtectedProperty1Access() = publicToProtectedProperty1
    fun publicToProtectedProperty2Access() = publicToProtectedProperty2
    fun publicToInternalProperty1Access() = publicToInternalProperty1
    fun publicToInternalProperty2Access() = publicToInternalProperty2
    fun publicToPrivateProperty1Access() = publicToPrivateProperty1
    fun publicToPrivateProperty2Access() = publicToPrivateProperty2

    // As far as protected members can't be accessed outside of the class hierarchy, we need special accessors.
    fun protectedToPublicProperty1Access() = protectedToPublicProperty1
    fun protectedToPublicProperty2Access() = protectedToPublicProperty2
    fun protectedToInternalProperty1Access() = protectedToInternalProperty1
    fun protectedToInternalProperty2Access() = protectedToInternalProperty2
    fun protectedToPrivateProperty1Access() = protectedToPrivateProperty1
    fun protectedToPrivateProperty2Access() = protectedToPrivateProperty2

    // Overridden properties with changed visibility:
    override val publicToProtectedOverriddenProperty1 = "ContainerImpl.publicToProtectedOverriddenProperty1"
    override val publicToProtectedOverriddenProperty2 get() = "ContainerImpl.publicToProtectedOverriddenProperty2"
    override val publicToProtectedOverriddenProperty3 get() = "ContainerImpl.publicToProtectedOverriddenProperty3"
    override val publicToProtectedOverriddenProperty4 = "ContainerImpl.publicToProtectedOverriddenProperty4"
    override val publicToInternalOverriddenProperty1 = "ContainerImpl.publicToInternalOverriddenProperty1"
    override val publicToInternalOverriddenProperty2 get() = "ContainerImpl.publicToInternalOverriddenProperty2"
    override val publicToInternalOverriddenProperty3 get() = "ContainerImpl.publicToInternalOverriddenProperty3"
    override val publicToInternalOverriddenProperty4 = "ContainerImpl.publicToInternalOverriddenProperty4"
    override val publicToPrivateOverriddenProperty1 = "ContainerImpl.publicToPrivateOverriddenProperty1"
    override val publicToPrivateOverriddenProperty2 get() = "ContainerImpl.publicToPrivateOverriddenProperty2"
    override val publicToPrivateOverriddenProperty3 get() = "ContainerImpl.publicToPrivateOverriddenProperty3"
    override val publicToPrivateOverriddenProperty4 = "ContainerImpl.publicToPrivateOverriddenProperty4"

    override val protectedToPublicOverriddenProperty1 = "ContainerImpl.protectedToPublicOverriddenProperty1"
    override val protectedToPublicOverriddenProperty2 get() = "ContainerImpl.protectedToPublicOverriddenProperty2"
    override val protectedToPublicOverriddenProperty3 get() = "ContainerImpl.protectedToPublicOverriddenProperty3"
    override val protectedToPublicOverriddenProperty4 = "ContainerImpl.protectedToPublicOverriddenProperty4"
    override val protectedToInternalOverriddenProperty1 = "ContainerImpl.protectedToInternalOverriddenProperty1"
    override val protectedToInternalOverriddenProperty2 get() = "ContainerImpl.protectedToInternalOverriddenProperty2"
    override val protectedToInternalOverriddenProperty3 get() = "ContainerImpl.protectedToInternalOverriddenProperty3"
    override val protectedToInternalOverriddenProperty4 = "ContainerImpl.protectedToInternalOverriddenProperty4"
    override val protectedToPrivateOverriddenProperty1 = "ContainerImpl.protectedToPrivateOverriddenProperty1"
    override val protectedToPrivateOverriddenProperty2 get() = "ContainerImpl.protectedToPrivateOverriddenProperty2"
    override val protectedToPrivateOverriddenProperty3 get() = "ContainerImpl.protectedToPrivateOverriddenProperty3"
    override val protectedToPrivateOverriddenProperty4 = "ContainerImpl.protectedToPrivateOverriddenProperty4"

    // As far as protected members can't be accessed outside of the class hierarchy, we need special accessors.
    fun protectedToPublicOverriddenProperty1Access() = protectedToPublicOverriddenProperty1
    fun protectedToPublicOverriddenProperty2Access() = protectedToPublicOverriddenProperty2
    fun protectedToPublicOverriddenProperty3Access() = protectedToPublicOverriddenProperty3
    fun protectedToPublicOverriddenProperty4Access() = protectedToPublicOverriddenProperty4
    fun protectedToInternalOverriddenProperty1Access() = protectedToInternalOverriddenProperty1
    fun protectedToInternalOverriddenProperty2Access() = protectedToInternalOverriddenProperty2
    fun protectedToInternalOverriddenProperty3Access() = protectedToInternalOverriddenProperty3
    fun protectedToInternalOverriddenProperty4Access() = protectedToInternalOverriddenProperty4
    fun protectedToPrivateOverriddenProperty1Access() = protectedToPrivateOverriddenProperty1
    fun protectedToPrivateOverriddenProperty2Access() = protectedToPrivateOverriddenProperty2
    fun protectedToPrivateOverriddenProperty3Access() = protectedToPrivateOverriddenProperty3
    fun protectedToPrivateOverriddenProperty4Access() = protectedToPrivateOverriddenProperty4
}
