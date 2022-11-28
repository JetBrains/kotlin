class ContainerImpl : Container() {
    // Just to check that accessing from within the class hierarchy has the same effect as accessing from the outside:
    fun publicToProtectedProperty1Access() = publicToProtectedProperty1
    fun publicToProtectedProperty2Access() = publicToProtectedProperty2
    fun publicToInternalProperty1Access() = publicToInternalProperty1
    fun publicToInternalProperty2Access() = publicToInternalProperty2
    fun publicToInternalPAProperty1Access() = publicToInternalPAProperty1
    fun publicToInternalPAProperty2Access() = publicToInternalPAProperty2
    fun publicToPrivateProperty1Access() = publicToPrivateProperty1
    fun publicToPrivateProperty2Access() = publicToPrivateProperty2

    // As far as protected members can't be accessed outside of the class hierarchy, we need special accessors.
    fun protectedToPublicProperty1Access() = protectedToPublicProperty1
    fun protectedToPublicProperty2Access() = protectedToPublicProperty2
    fun protectedToInternalProperty1Access() = protectedToInternalProperty1
    fun protectedToInternalProperty2Access() = protectedToInternalProperty2
    fun protectedToInternalPAProperty1Access() = protectedToInternalPAProperty1
    fun protectedToInternalPAProperty2Access() = protectedToInternalPAProperty2
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
    override val publicToInternalPAOverriddenProperty1 = "ContainerImpl.publicToInternalPAOverriddenProperty1"
    override val publicToInternalPAOverriddenProperty2 get() = "ContainerImpl.publicToInternalPAOverriddenProperty2"
    override val publicToInternalPAOverriddenProperty3 get() = "ContainerImpl.publicToInternalPAOverriddenProperty3"
    override val publicToInternalPAOverriddenProperty4 = "ContainerImpl.publicToInternalPAOverriddenProperty4"
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
    override val protectedToInternalPAOverriddenProperty1 = "ContainerImpl.protectedToInternalPAOverriddenProperty1"
    override val protectedToInternalPAOverriddenProperty2 get() = "ContainerImpl.protectedToInternalPAOverriddenProperty2"
    override val protectedToInternalPAOverriddenProperty3 get() = "ContainerImpl.protectedToInternalPAOverriddenProperty3"
    override val protectedToInternalPAOverriddenProperty4 = "ContainerImpl.protectedToInternalPAOverriddenProperty4"
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
    fun protectedToInternalPAOverriddenProperty1Access() = protectedToInternalPAOverriddenProperty1
    fun protectedToInternalPAOverriddenProperty2Access() = protectedToInternalPAOverriddenProperty2
    fun protectedToInternalPAOverriddenProperty3Access() = protectedToInternalPAOverriddenProperty3
    fun protectedToInternalPAOverriddenProperty4Access() = protectedToInternalPAOverriddenProperty4
    fun protectedToPrivateOverriddenProperty1Access() = protectedToPrivateOverriddenProperty1
    fun protectedToPrivateOverriddenProperty2Access() = protectedToPrivateOverriddenProperty2
    fun protectedToPrivateOverriddenProperty3Access() = protectedToPrivateOverriddenProperty3
    fun protectedToPrivateOverriddenProperty4Access() = protectedToPrivateOverriddenProperty4

    // Properties that accedentally start to override/conflict with properties added to Container since version v2:
    public val newPublicProperty1 = "ContainerImpl.newPublicProperty1"
    public val newPublicProperty2 get() = "ContainerImpl.newPublicProperty2"
    public val newPublicProperty3 get() = "ContainerImpl.newPublicProperty3"
    public val newPublicProperty4 = "ContainerImpl.newPublicProperty4"
    public open val newOpenPublicProperty1 = "ContainerImpl.newOpenPublicProperty1"
    public open val newOpenPublicProperty2 get() = "ContainerImpl.newOpenPublicProperty2"
    public open val newOpenPublicProperty3 get() = "ContainerImpl.newOpenPublicProperty3"
    public open val newOpenPublicProperty4 = "ContainerImpl.newOpenPublicProperty4"
    protected val newProtectedProperty1 = "ContainerImpl.newProtectedProperty1"
    protected val newProtectedProperty2 get() = "ContainerImpl.newProtectedProperty2"
    protected val newProtectedProperty3 get() = "ContainerImpl.newProtectedProperty3"
    protected val newProtectedProperty4 = "ContainerImpl.newProtectedProperty4"
    protected open val newOpenProtectedProperty1 = "ContainerImpl.newOpenProtectedProperty1"
    protected open val newOpenProtectedProperty2 get() = "ContainerImpl.newOpenProtectedProperty2"
    protected open val newOpenProtectedProperty3 get() = "ContainerImpl.newOpenProtectedProperty3"
    protected open val newOpenProtectedProperty4 = "ContainerImpl.newOpenProtectedProperty4"
    internal val newInternalProperty1 = "ContainerImpl.newInternalProperty1"
    internal val newInternalProperty2 get() = "ContainerImpl.newInternalProperty2"
    internal val newInternalProperty3 get() = "ContainerImpl.newInternalProperty3"
    internal val newInternalProperty4 = "ContainerImpl.newInternalProperty4"
    internal open val newOpenInternalProperty1 = "ContainerImpl.newOpenInternalProperty1"
    internal open val newOpenInternalProperty2 get() = "ContainerImpl.newOpenInternalProperty2"
    internal open val newOpenInternalProperty3 get() = "ContainerImpl.newOpenInternalProperty3"
    internal open val newOpenInternalProperty4 = "ContainerImpl.newOpenInternalProperty4"
    internal val newInternalPAProperty1 = "ContainerImpl.newInternalPAProperty1"
    internal val newInternalPAProperty2 get() = "ContainerImpl.newInternalPAProperty2"
    internal val newInternalPAProperty3 get() = "ContainerImpl.newInternalPAProperty3"
    internal val newInternalPAProperty4 = "ContainerImpl.newInternalPAProperty4"
    internal open val newOpenInternalPAProperty1 = "ContainerImpl.newOpenInternalPAProperty1"
    internal open val newOpenInternalPAProperty2 get() = "ContainerImpl.newOpenInternalPAProperty2"
    internal open val newOpenInternalPAProperty3 get() = "ContainerImpl.newOpenInternalPAProperty3"
    internal open val newOpenInternalPAProperty4 = "ContainerImpl.newOpenInternalPAProperty4"
    private val newPrivateProperty1 = "ContainerImpl.newPrivateProperty1"
    private val newPrivateProperty2 get() = "ContainerImpl.newPrivateProperty2"
    private val newPrivateProperty3 get() = "ContainerImpl.newPrivateProperty3"
    private val newPrivateProperty4 = "ContainerImpl.newPrivateProperty4"

    // As far as protected/private members can't be accessed outside of the class hierarchy, and internal can't be accessed
    // outside of module, we need special accessors.
    fun newProtectedProperty1Access() = newProtectedProperty1
    fun newProtectedProperty2Access() = newProtectedProperty2
    fun newProtectedProperty3Access() = newProtectedProperty3
    fun newProtectedProperty4Access() = newProtectedProperty4
    fun newOpenProtectedProperty1Access() = newOpenProtectedProperty1
    fun newOpenProtectedProperty2Access() = newOpenProtectedProperty2
    fun newOpenProtectedProperty3Access() = newOpenProtectedProperty3
    fun newOpenProtectedProperty4Access() = newOpenProtectedProperty4
    fun newInternalProperty1Access() = newInternalProperty1
    fun newInternalProperty2Access() = newInternalProperty2
    fun newInternalProperty3Access() = newInternalProperty3
    fun newInternalProperty4Access() = newInternalProperty4
    fun newOpenInternalProperty1Access() = newOpenInternalProperty1
    fun newOpenInternalProperty2Access() = newOpenInternalProperty2
    fun newOpenInternalProperty3Access() = newOpenInternalProperty3
    fun newOpenInternalProperty4Access() = newOpenInternalProperty4
    fun newInternalPAProperty1Access() = newInternalPAProperty1
    fun newInternalPAProperty2Access() = newInternalPAProperty2
    fun newInternalPAProperty3Access() = newInternalPAProperty3
    fun newInternalPAProperty4Access() = newInternalPAProperty4
    fun newOpenInternalPAProperty1Access() = newOpenInternalPAProperty1
    fun newOpenInternalPAProperty2Access() = newOpenInternalPAProperty2
    fun newOpenInternalPAProperty3Access() = newOpenInternalPAProperty3
    fun newOpenInternalPAProperty4Access() = newOpenInternalPAProperty4
    fun newPrivateProperty1Access() = newPrivateProperty1
    fun newPrivateProperty2Access() = newPrivateProperty2
    fun newPrivateProperty3Access() = newPrivateProperty3
    fun newPrivateProperty4Access() = newPrivateProperty4
}
