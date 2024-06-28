import abitestutils.abiTest

fun box() = abiTest {
    expectSuccess("OpenClass1(x=1)") { getOpenClassRemovedTA(1).toString() }
    expectSuccess("setOpenClassRemovedTA") { setOpenClassRemovedTA(null) }
    expectSuccess("OpenClassRemovedTAImpl(x=-2)") { getOpenClassRemovedTAImpl(2).toString() }
    expectSuccess("setOpenClassRemovedTAImpl") { setOpenClassRemovedTAImpl(null) }
    expectSuccess("OpenClassRemovedTATypeParameterHolder(t=OpenClass1(x=3))") { getOpenClassRemovedTATypeParameterHolder1(3).toString() }
    expectSuccess("OpenClassRemovedTATypeParameterHolder(t=OpenClassRemovedTAImpl(x=-4))") { getOpenClassRemovedTATypeParameterHolder2(4).toString() }
    expectSuccess("setOpenClassRemovedTATypeParameterHolder1") { setOpenClassRemovedTATypeParameterHolder1(null) }
    expectSuccess("setOpenClassRemovedTATypeParameterHolder2") { setOpenClassRemovedTATypeParameterHolder2(null) }
    expectSuccess("OpenClassRemovedTAImplTypeParameterHolder(t=OpenClassRemovedTAImpl(x=-5))") { getOpenClassRemovedTAImplTypeParameterHolder(5).toString() }
    expectSuccess("setOpenClassRemovedTAImplTypeParameterHolder") { setOpenClassRemovedTAImplTypeParameterHolder(null) }

    expectSuccess("OpenClass1(x=1)") { getOpenClassChangedTA(1).toString() }
    expectSuccess("setOpenClassChangedTA") { setOpenClassChangedTA(null) }
    expectSuccess("OpenClassChangedTAImpl(x=-2)") { getOpenClassChangedTAImpl(2).toString() }
    expectSuccess("setOpenClassChangedTAImpl") { setOpenClassChangedTAImpl(null) }
    expectSuccess("OpenClassChangedTATypeParameterHolder(t=OpenClass1(x=3))") { getOpenClassChangedTATypeParameterHolder1(3).toString() }
    expectSuccess("OpenClassChangedTATypeParameterHolder(t=OpenClassChangedTAImpl(x=-4))") { getOpenClassChangedTATypeParameterHolder2(4).toString() }
    expectSuccess("setOpenClassChangedTATypeParameterHolder1") { setOpenClassChangedTATypeParameterHolder1(null) }
    expectSuccess("setOpenClassChangedTATypeParameterHolder2") { setOpenClassChangedTATypeParameterHolder2(null) }
    expectSuccess("OpenClassChangedTAImplTypeParameterHolder(t=OpenClassChangedTAImpl(x=-5))") { getOpenClassChangedTAImplTypeParameterHolder(5).toString() }
    expectSuccess("setOpenClassChangedTAImplTypeParameterHolder") { setOpenClassChangedTAImplTypeParameterHolder(null) }

    expectSuccess("OpenClass1(x=1)") { getOpenClassNarrowedVisibilityTA(1).toString() }
    expectSuccess("setOpenClassNarrowedVisibilityTA") { setOpenClassNarrowedVisibilityTA(null) }
    expectSuccess("OpenClassNarrowedVisibilityTAImpl(x=-2)") { getOpenClassNarrowedVisibilityTAImpl(2).toString() }
    expectSuccess("setOpenClassNarrowedVisibilityTAImpl") { setOpenClassNarrowedVisibilityTAImpl(null) }
    expectSuccess("OpenClassNarrowedVisibilityTATypeParameterHolder(t=OpenClass1(x=3))") { getOpenClassNarrowedVisibilityTATypeParameterHolder1(3).toString() }
    expectSuccess("OpenClassNarrowedVisibilityTATypeParameterHolder(t=OpenClassNarrowedVisibilityTAImpl(x=-4))") { getOpenClassNarrowedVisibilityTATypeParameterHolder2(4).toString() }
    expectSuccess("setOpenClassNarrowedVisibilityTATypeParameterHolder1") { setOpenClassNarrowedVisibilityTATypeParameterHolder1(null) }
    expectSuccess("setOpenClassNarrowedVisibilityTATypeParameterHolder2") { setOpenClassNarrowedVisibilityTATypeParameterHolder2(null) }
    expectSuccess("OpenClassNarrowedVisibilityTAImplTypeParameterHolder(t=OpenClassNarrowedVisibilityTAImpl(x=-5))") { getOpenClassNarrowedVisibilityTAImplTypeParameterHolder(5).toString() }
    expectSuccess("setOpenClassNarrowedVisibilityTAImplTypeParameterHolder") { setOpenClassNarrowedVisibilityTAImplTypeParameterHolder(null) }
}
