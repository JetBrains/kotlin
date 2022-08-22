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
}
