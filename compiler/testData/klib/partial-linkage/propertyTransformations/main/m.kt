import abitestutils.abiTest

fun box() = abiTest {
    val oci: OpenClassImpl = OpenClassImpl()
    val oc: OpenClass = oci

    expectSuccess("OpenClassV2.openNonInlineToInlineProperty") { openNonInlineToInlinePropertyInOpenClass(oc) }
    expectSuccess("OpenClassV2.openNonInlineToInlinePropertyWithDelegation") { openNonInlineToInlinePropertyWithDelegationInOpenClass(oc) }
    expectSuccess("OpenClassV2.newInlineProperty1") { newInlineProperty1InOpenClass(oc) }
    expectSuccess("OpenClassV2.newInlineProperty2") { newInlineProperty2InOpenClass(oc) }
    expectSuccess( // TODO: this should be fixed in JS, KT-56762
        if (testMode.isJs) "OpenClassImpl.newNonInlineProperty" else "OpenClassV2.newNonInlineProperty"
    ) { newNonInlinePropertyInOpenClass(oc) }

    expectSuccess("OpenClassImpl.openNonInlineToInlineProperty") { openNonInlineToInlinePropertyInOpenClassImpl(oci) }
    expectSuccess("OpenClassV2.openNonInlineToInlinePropertyWithDelegation called from OpenClassImpl.openNonInlineToInlinePropertyWithDelegation") { openNonInlineToInlinePropertyWithDelegationInOpenClassImpl(oci) }
    expectSuccess("OpenClassImpl.newInlineProperty1") { newInlineProperty1InOpenClassImpl(oci) }
    expectSuccess("OpenClassImpl.newInlineProperty2") { newInlineProperty2InOpenClassImpl(oci) }
    expectSuccess("OpenClassImpl.newNonInlineProperty") { newNonInlinePropertyInOpenClassImpl(oci) }

    expectSuccess("OpenClassV2.openNonInlineToInlineProperty=a") { openNonInlineToInlinePropertyInOpenClass(oc, "a") }
    expectSuccess("OpenClassV2.openNonInlineToInlinePropertyWithDelegation=b") { openNonInlineToInlinePropertyWithDelegationInOpenClass(oc, "b") }
    expectSuccess("OpenClassV2.newInlineProperty1=c") { newInlineProperty1InOpenClass(oc, "c") }
    expectSuccess("OpenClassV2.newInlineProperty2=d") { newInlineProperty2InOpenClass(oc, "d") }
    expectSuccess( // TODO: this should be fixed in JS, KT-56762
        if (testMode.isJs) "OpenClassImpl.newNonInlineProperty=e" else "OpenClassV2.newNonInlineProperty=e"
    ) { newNonInlinePropertyInOpenClass(oc, "e") }

    expectSuccess("OpenClassImpl.openNonInlineToInlineProperty=f") { openNonInlineToInlinePropertyInOpenClassImpl(oci, "f") }
    expectSuccess("OpenClassV2.openNonInlineToInlinePropertyWithDelegation=h called from OpenClassImpl.openNonInlineToInlinePropertyWithDelegation") { openNonInlineToInlinePropertyWithDelegationInOpenClassImpl(oci, "h") }
    expectSuccess("OpenClassImpl.newInlineProperty1=i") { newInlineProperty1InOpenClassImpl(oci, "i") }
    expectSuccess("OpenClassImpl.newInlineProperty2=j") { newInlineProperty2InOpenClassImpl(oci, "j") }
    expectSuccess("OpenClassImpl.newNonInlineProperty=k") { newNonInlinePropertyInOpenClassImpl(oci, "k") }
}
