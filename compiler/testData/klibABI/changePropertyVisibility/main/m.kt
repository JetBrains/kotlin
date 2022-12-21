import abitestutils.abiTest
import abitestutils.TestBuilder
import abitestutils.TestMode.NATIVE_CACHE_STATIC_EVERYWHERE

fun box() = abiTest {
    val c = Container()
    val ci = ContainerImpl()

    success("publicToInternalTopLevelProperty1.v2") { publicToInternalTopLevelProperty1 } // Signature remains the same.
    success("publicToInternalTopLevelProperty2.v2") { publicToInternalTopLevelProperty2 } // Signature remains the same.
    success("publicToInternalPATopLevelProperty1.v2") { publicToInternalPATopLevelProperty1 } // Signature remains the same.
    success("publicToInternalPATopLevelProperty2.v2") { publicToInternalPATopLevelProperty2 } // Signature remains the same.
    unlinkedSymbol("/publicToPrivateTopLevelProperty1.<get-publicToPrivateTopLevelProperty1>") { publicToPrivateTopLevelProperty1 } // Signature changed.
    unlinkedSymbol("/publicToPrivateTopLevelProperty2.<get-publicToPrivateTopLevelProperty2>") { publicToPrivateTopLevelProperty2 } // Signature changed.

    success("Container.publicToProtectedProperty1.v2") { c.publicToProtectedProperty1 } // Signature remains the same.
    success("Container.publicToProtectedProperty1.v2") { ci.publicToProtectedProperty1 } // Signature remains the same.
    success("Container.publicToProtectedProperty2.v2") { c.publicToProtectedProperty2 } // Signature remains the same.
    success("Container.publicToProtectedProperty2.v2") { ci.publicToProtectedProperty2 } // Signature remains the same.
    // TODO: KT-54469, Container.publicToInternalProperty1 should fail because it is accessed from another module.
    success("Container.publicToInternalProperty1.v2") { c.publicToInternalProperty1 } // Signature remains the same.
    unlinkedSymbol("/ContainerImpl.publicToInternalProperty1.<get-publicToInternalProperty1>") { ci.publicToInternalProperty1 } // FOs are not generated for internal members from other module.
    // TODO: KT-54469, Container.publicToInternalProperty2 should fail because it is accessed from another module.
    success("Container.publicToInternalProperty2.v2") { c.publicToInternalProperty2 } // Signature remains the same.
    unlinkedSymbol("/ContainerImpl.publicToInternalProperty2.<get-publicToInternalProperty2>") { ci.publicToInternalProperty2 }  // FOs are not generated for internal members from other module.
    // TODO: KT-54469, Container.publicToInternalPAProperty1 should fail because it is accessed from another module.
    success("Container.publicToInternalPAProperty1.v2") { c.publicToInternalPAProperty1 } // Signature remains the same.
    unlinkedSymbol("/ContainerImpl.publicToInternalPAProperty1.<get-publicToInternalPAProperty1>") { ci.publicToInternalPAProperty1 }  // FOs are not generated for internal members from other module.
    // TODO: KT-54469, Container.publicToInternalPAProperty2 should fail because it is accessed from another module.
    success("Container.publicToInternalPAProperty2.v2") { c.publicToInternalPAProperty2 } // Signature remains the same.
    unlinkedSymbol("/ContainerImpl.publicToInternalPAProperty2.<get-publicToInternalPAProperty2>") { ci.publicToInternalPAProperty2 }  // FOs are not generated for internal members from other module.
    inaccessible("publicToPrivateProperty1.<get-publicToPrivateProperty1>") { c.publicToPrivateProperty1 } // Inaccessible from other module though signature remains the same.
    unlinkedSymbol("/ContainerImpl.publicToPrivateProperty1.<get-publicToPrivateProperty1>") { ci.publicToPrivateProperty1 } // FOs are not generated for private members.
    inaccessible("publicToPrivateProperty2.<get-publicToPrivateProperty2>") { c.publicToPrivateProperty2 } // Inaccessible from other module though signature remains the same.
    unlinkedSymbol("/ContainerImpl.publicToPrivateProperty2.<get-publicToPrivateProperty2>") { ci.publicToPrivateProperty2 } // FOs are not generated for private members.

    success("Container.publicToProtectedProperty1.v2") { ci.publicToProtectedProperty1Access() } // Signature remains the same.
    success("Container.publicToProtectedProperty2.v2") { ci.publicToProtectedProperty2Access() } // Signature remains the same.
    unlinkedSymbol("/ContainerImpl.publicToInternalProperty1.<get-publicToInternalProperty1>") { ci.publicToInternalProperty1Access() } // FOs are not generated for internal members from other module.
    unlinkedSymbol("/ContainerImpl.publicToInternalProperty2.<get-publicToInternalProperty2>") { ci.publicToInternalProperty2Access() } // FOs are not generated for internal members from other module.
    unlinkedSymbol("/ContainerImpl.publicToInternalPAProperty1.<get-publicToInternalPAProperty1>") { ci.publicToInternalPAProperty1Access() } // FOs are not generated for internal members from other module.
    unlinkedSymbol("/ContainerImpl.publicToInternalPAProperty2.<get-publicToInternalPAProperty2>") { ci.publicToInternalPAProperty2Access() } // FOs are not generated for internal members from other module.
    unlinkedSymbol("/ContainerImpl.publicToPrivateProperty1.<get-publicToPrivateProperty1>") { ci.publicToPrivateProperty1Access() } // FOs are not generated for private members.
    unlinkedSymbol("/ContainerImpl.publicToPrivateProperty2.<get-publicToPrivateProperty2>") { ci.publicToPrivateProperty2Access() } // FOs are not generated for private members.
    success("Container.protectedToPublicProperty1.v2") { ci.protectedToPublicProperty1Access() } // Signature remains the same.
    success("Container.protectedToPublicProperty2.v2") { ci.protectedToPublicProperty2Access() } // Signature remains the same.
    unlinkedSymbol("/ContainerImpl.protectedToInternalProperty1.<get-protectedToInternalProperty1>") { ci.protectedToInternalProperty1Access() } // FOs are not generated for internal members from other module.
    unlinkedSymbol("/ContainerImpl.protectedToInternalProperty2.<get-protectedToInternalProperty2>") { ci.protectedToInternalProperty2Access() } // FOs are not generated for internal members from other module.
    unlinkedSymbol("/ContainerImpl.protectedToInternalPAProperty1.<get-protectedToInternalPAProperty1>") { ci.protectedToInternalPAProperty1Access() } // FOs are not generated for internal members from other module.
    unlinkedSymbol("/ContainerImpl.protectedToInternalPAProperty2.<get-protectedToInternalPAProperty2>") { ci.protectedToInternalPAProperty2Access() } // FOs are not generated for internal members from other module.
    unlinkedSymbol("/ContainerImpl.protectedToPrivateProperty1.<get-protectedToPrivateProperty1>") { ci.protectedToPrivateProperty1Access() } // FOs are not generated for private members.
    unlinkedSymbol("/ContainerImpl.protectedToPrivateProperty2.<get-protectedToPrivateProperty2>") { ci.protectedToPrivateProperty2Access() } // FOs are not generated for private members.

    success("ContainerImpl.publicToProtectedOverriddenProperty1") { ci.publicToProtectedOverriddenProperty1 }
    success("ContainerImpl.publicToProtectedOverriddenProperty2") { ci.publicToProtectedOverriddenProperty2 }
    success("ContainerImpl.publicToProtectedOverriddenProperty3") { ci.publicToProtectedOverriddenProperty3 }
    success("ContainerImpl.publicToProtectedOverriddenProperty4") { ci.publicToProtectedOverriddenProperty4 }
    success("ContainerImpl.publicToInternalOverriddenProperty1") { ci.publicToInternalOverriddenProperty1 }
    success("ContainerImpl.publicToInternalOverriddenProperty2") { ci.publicToInternalOverriddenProperty2 }
    success("ContainerImpl.publicToInternalOverriddenProperty3") { ci.publicToInternalOverriddenProperty3 }
    success("ContainerImpl.publicToInternalOverriddenProperty4") { ci.publicToInternalOverriddenProperty4 }
    success("ContainerImpl.publicToInternalPAOverriddenProperty1") { ci.publicToInternalPAOverriddenProperty1 }
    success("ContainerImpl.publicToInternalPAOverriddenProperty2") { ci.publicToInternalPAOverriddenProperty2 }
    success("ContainerImpl.publicToInternalPAOverriddenProperty3") { ci.publicToInternalPAOverriddenProperty3 }
    success("ContainerImpl.publicToInternalPAOverriddenProperty4") { ci.publicToInternalPAOverriddenProperty4 }
    success("ContainerImpl.publicToPrivateOverriddenProperty1") { ci.publicToPrivateOverriddenProperty1 }
    success("ContainerImpl.publicToPrivateOverriddenProperty2") { ci.publicToPrivateOverriddenProperty2 }
    success("ContainerImpl.publicToPrivateOverriddenProperty3") { ci.publicToPrivateOverriddenProperty3 }
    success("ContainerImpl.publicToPrivateOverriddenProperty4") { ci.publicToPrivateOverriddenProperty4 }
    success("ContainerImpl.protectedToPublicOverriddenProperty1") { ci.protectedToPublicOverriddenProperty1Access() }
    success("ContainerImpl.protectedToPublicOverriddenProperty2") { ci.protectedToPublicOverriddenProperty2Access() }
    success("ContainerImpl.protectedToPublicOverriddenProperty3") { ci.protectedToPublicOverriddenProperty3Access() }
    success("ContainerImpl.protectedToPublicOverriddenProperty4") { ci.protectedToPublicOverriddenProperty4Access() }
    success("ContainerImpl.protectedToInternalOverriddenProperty1") { ci.protectedToInternalOverriddenProperty1Access() }
    success("ContainerImpl.protectedToInternalOverriddenProperty2") { ci.protectedToInternalOverriddenProperty2Access() }
    success("ContainerImpl.protectedToInternalOverriddenProperty3") { ci.protectedToInternalOverriddenProperty3Access() }
    success("ContainerImpl.protectedToInternalOverriddenProperty4") { ci.protectedToInternalOverriddenProperty4Access() }
    success("ContainerImpl.protectedToInternalPAOverriddenProperty1") { ci.protectedToInternalPAOverriddenProperty1Access() }
    success("ContainerImpl.protectedToInternalPAOverriddenProperty2") { ci.protectedToInternalPAOverriddenProperty2Access() }
    success("ContainerImpl.protectedToInternalPAOverriddenProperty3") { ci.protectedToInternalPAOverriddenProperty3Access() }
    success("ContainerImpl.protectedToInternalPAOverriddenProperty4") { ci.protectedToInternalPAOverriddenProperty4Access() }
    success("ContainerImpl.protectedToPrivateOverriddenProperty1") { ci.protectedToPrivateOverriddenProperty1Access() }
    success("ContainerImpl.protectedToPrivateOverriddenProperty2") { ci.protectedToPrivateOverriddenProperty2Access() }
    success("ContainerImpl.protectedToPrivateOverriddenProperty3") { ci.protectedToPrivateOverriddenProperty3Access() }
    success("ContainerImpl.protectedToPrivateOverriddenProperty4") { ci.protectedToPrivateOverriddenProperty4Access() }

    success("ContainerImpl.newPublicProperty1") { ci.newPublicProperty1 }
    success("ContainerImpl.newPublicProperty2") { ci.newPublicProperty2 }
    success("ContainerImpl.newPublicProperty3") { ci.newPublicProperty3 }
    success("ContainerImpl.newPublicProperty4") { ci.newPublicProperty4 }
    success("ContainerImpl.newProtectedProperty1") { ci.newProtectedProperty1Access() }
    success("ContainerImpl.newProtectedProperty2") { ci.newProtectedProperty2Access() }
    success("ContainerImpl.newProtectedProperty3") { ci.newProtectedProperty3Access() }
    success("ContainerImpl.newProtectedProperty4") { ci.newProtectedProperty4Access() }
    success("ContainerImpl.newOpenProtectedProperty1") { ci.newOpenProtectedProperty1Access() }
    success("ContainerImpl.newOpenProtectedProperty2") { ci.newOpenProtectedProperty2Access() }
    success("ContainerImpl.newOpenProtectedProperty3") { ci.newOpenProtectedProperty3Access() }
    success("ContainerImpl.newOpenProtectedProperty4") { ci.newOpenProtectedProperty4Access() }
    success("ContainerImpl.newInternalProperty1") { ci.newInternalProperty1Access() }
    success("ContainerImpl.newInternalProperty2") { ci.newInternalProperty2Access() }
    success("ContainerImpl.newInternalProperty3") { ci.newInternalProperty3Access() }
    success("ContainerImpl.newInternalProperty4") { ci.newInternalProperty4Access() }
    success("ContainerImpl.newOpenInternalProperty1") { ci.newOpenInternalProperty1Access() }
    success("ContainerImpl.newOpenInternalProperty2") { ci.newOpenInternalProperty2Access() }
    success("ContainerImpl.newOpenInternalProperty3") { ci.newOpenInternalProperty3Access() }
    success("ContainerImpl.newOpenInternalProperty4") { ci.newOpenInternalProperty4Access() }
    success("ContainerImpl.newInternalPAProperty1") { ci.newInternalPAProperty1Access() }
    success("ContainerImpl.newInternalPAProperty2") { ci.newInternalPAProperty2Access() }
    success("ContainerImpl.newInternalPAProperty3") { ci.newInternalPAProperty3Access() }
    success("ContainerImpl.newInternalPAProperty4") { ci.newInternalPAProperty4Access() }
    success("ContainerImpl.newOpenInternalPAProperty1") { ci.newOpenInternalPAProperty1Access() }
    success("ContainerImpl.newOpenInternalPAProperty2") { ci.newOpenInternalPAProperty2Access() }
    success("ContainerImpl.newOpenInternalPAProperty3") { ci.newOpenInternalPAProperty3Access() }
    success("ContainerImpl.newOpenInternalPAProperty4") { ci.newOpenInternalPAProperty4Access() }
    success("ContainerImpl.newPrivateProperty1") { ci.newPrivateProperty1Access() }
    success("ContainerImpl.newPrivateProperty2") { ci.newPrivateProperty2Access() }
    success("ContainerImpl.newPrivateProperty3") { ci.newPrivateProperty3Access() }
    success("ContainerImpl.newPrivateProperty4") { ci.newPrivateProperty4Access() }
}

// Shortcuts:
private inline fun TestBuilder.success(expectedOutcome: String, noinline block: () -> String) =
    expectSuccess(expectedOutcome, block)

private inline fun TestBuilder.unlinkedSymbol(signature: String, noinline block: () -> Unit) {
    val accessorName = signature.removePrefix("/").split('.').takeLast(2).joinToString(".")
    expectFailure(linkage("Property accessor '$accessorName' can not be called: No property accessor found for symbol '$signature'"), block)
}

private inline fun TestBuilder.inaccessible(accessorName: String, noinline block: () -> Unit) = expectFailure(
    linkage("Property accessor '$accessorName' can not be called: Private property accessor declared in module <lib1> can not be accessed in module <main>"),
    block
)
