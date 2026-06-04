import abitestutils.abiTest

fun box() = abiTest {
    // open class
    expectFailure(
        linkage("Function 'getOpenClassRemovedTA' can not be called: Function uses unlinked class symbol '/Foo'")
    ) { getOpenClassRemovedTA(1).toString() }

    expectFailure(
        linkage("Function 'setOpenClassRemovedTA' can not be called: Function uses unlinked class symbol '/Foo'")
    ) { setOpenClassRemovedTA(null) }

    expectFailure(
        linkage("Function 'getOpenClassRemovedTAImpl' can not be called: Function uses unlinked class symbol '/Foo'")
    ) { getOpenClassRemovedTAImpl(2).toString() }

    expectFailure(
        linkage("Function 'setOpenClassRemovedTAImpl' can not be called: Function uses unlinked class symbol '/Foo' (via class 'OpenClassRemovedTAImpl')")
    ) { setOpenClassRemovedTAImpl(null) }

    expectFailure(
        linkage("Function 'getOpenClassRemovedTATypeParameterHolder1' can not be called: Function uses unlinked class symbol '/Foo' (via data class 'OpenClassRemovedTATypeParameterHolder')")
    ) { getOpenClassRemovedTATypeParameterHolder1(3).toString() }

    expectFailure(
        linkage("Function 'getOpenClassRemovedTATypeParameterHolder2' can not be called: Function uses unlinked class symbol '/Foo' (via data class 'OpenClassRemovedTATypeParameterHolder')")
    ) { getOpenClassRemovedTATypeParameterHolder2(4).toString() }

    expectFailure(
        linkage("Function 'setOpenClassRemovedTATypeParameterHolder1' can not be called: Function uses unlinked class symbol '/Foo' (via data class 'OpenClassRemovedTATypeParameterHolder')")
    ) { setOpenClassRemovedTATypeParameterHolder1(null) }

    expectFailure(
        linkage("Function 'setOpenClassRemovedTATypeParameterHolder2' can not be called: Function uses unlinked class symbol '/Foo' (via data class 'OpenClassRemovedTATypeParameterHolder')")
    ) { setOpenClassRemovedTATypeParameterHolder2(null) }

    expectFailure(
        linkage("Function 'getOpenClassRemovedTAImplTypeParameterHolder' can not be called: Function uses unlinked class symbol '/Foo' (via data class 'OpenClassRemovedTAImplTypeParameterHolder')")
    ) { getOpenClassRemovedTAImplTypeParameterHolder(5).toString() }

    expectFailure(
        linkage("Function 'setOpenClassRemovedTAImplTypeParameterHolder' can not be called: Function uses unlinked class symbol '/Foo' (via data class 'OpenClassRemovedTAImplTypeParameterHolder')")
    ) { setOpenClassRemovedTAImplTypeParameterHolder(null) }

    // interface
    expectFailure(
        linkage("Function 'getInterfaceRemovedTA' can not be called: Function uses unlinked class symbol '/Bar'")
    ) { getInterfaceRemovedTA(1).toString() }

    expectFailure(
        linkage("Function 'setInterfaceRemovedTA' can not be called: Function uses unlinked class symbol '/Bar'")
    ) { setInterfaceRemovedTA(null) }

    expectFailure(
        linkage("Function 'getInterfaceRemovedTAImpl' can not be called: Function uses unlinked class symbol '/Bar'")
    ) { getInterfaceRemovedTAImpl(2).toString() }

    expectFailure(
        linkage("Function 'setInterfaceRemovedTAImpl' can not be called: Function uses unlinked class symbol '/Bar' (via class 'InterfaceRemovedTAImpl')")
    ) { setInterfaceRemovedTAImpl(null) }

    expectFailure(
        linkage("Function 'getInterfaceRemovedTATypeParameterHolder1' can not be called: Function uses unlinked class symbol '/Bar' (via data class 'InterfaceRemovedTATypeParameterHolder')")
    ) { getInterfaceRemovedTATypeParameterHolder1(3).toString() }

    expectFailure(
        linkage("Function 'getInterfaceRemovedTATypeParameterHolder2' can not be called: Function uses unlinked class symbol '/Bar' (via data class 'InterfaceRemovedTATypeParameterHolder')")
    ) { getInterfaceRemovedTATypeParameterHolder2(4).toString() }

    expectFailure(
        linkage("Function 'setInterfaceRemovedTATypeParameterHolder1' can not be called: Function uses unlinked class symbol '/Bar' (via data class 'InterfaceRemovedTATypeParameterHolder')")
    ) { setInterfaceRemovedTATypeParameterHolder1(null) }

    expectFailure(
        linkage("Function 'setInterfaceRemovedTATypeParameterHolder2' can not be called: Function uses unlinked class symbol '/Bar' (via data class 'InterfaceRemovedTATypeParameterHolder')")
    ) { setInterfaceRemovedTATypeParameterHolder2(null) }

    expectFailure(
        linkage("Function 'getInterfaceRemovedTAImplTypeParameterHolder' can not be called: Function uses unlinked class symbol '/Bar' (via data class 'InterfaceRemovedTAImplTypeParameterHolder')")
    ) { getInterfaceRemovedTAImplTypeParameterHolder(5).toString() }

    expectFailure(
        linkage("Function 'setInterfaceRemovedTAImplTypeParameterHolder' can not be called: Function uses unlinked class symbol '/Bar' (via data class 'InterfaceRemovedTAImplTypeParameterHolder')")
    ) { setInterfaceRemovedTAImplTypeParameterHolder(null) }

    // with type parameter
    expectFailure(
        linkage("Function 'getWithTypeParameterRemovedTA' can not be called: Function uses unlinked class symbol '/WithTypeParameter'")
    ) { getWithTypeParameterRemovedTA(1).toString() }

    expectFailure(
        linkage("Function 'setWithTypeParameterRemovedTA' can not be called: Function uses unlinked class symbol '/WithTypeParameter'")
    ) { setWithTypeParameterRemovedTA<Int>(null) }

    expectFailure(
        linkage("Function 'getWithTypeParameterRemovedTAImpl' can not be called: Function uses unlinked class symbol '/WithTypeParameter'")
    ) { getWithTypeParameterRemovedTAImpl(2).toString() }

    expectFailure(
        linkage("Function 'setWithTypeParameterRemovedTAImpl' can not be called: Function uses unlinked class symbol '/WithTypeParameter' (via class 'WithTypeParameterRemovedTAImpl')")
    ) { setWithTypeParameterRemovedTAImpl<Int>(null) }

    expectFailure(
        linkage("Function 'getWithTypeParameterRemovedTATypeParameterHolder1' can not be called: Function uses unlinked class symbol '/WithTypeParameter' (via data class 'WithTypeParameterRemovedTATypeParameterHolder')")
    ) { getWithTypeParameterRemovedTATypeParameterHolder1(3).toString() }

    expectFailure(
        linkage("Function 'getWithTypeParameterRemovedTATypeParameterHolder2' can not be called: Function uses unlinked class symbol '/WithTypeParameter' (via data class 'WithTypeParameterRemovedTATypeParameterHolder')")
    ) { getWithTypeParameterRemovedTATypeParameterHolder2(4).toString() }

    expectFailure(
        linkage("Function 'setWithTypeParameterRemovedTATypeParameterHolder1' can not be called: Function uses unlinked class symbol '/WithTypeParameter' (via data class 'WithTypeParameterRemovedTATypeParameterHolder')")
    ) { setWithTypeParameterRemovedTATypeParameterHolder1(null) }

    expectFailure(
        linkage("Function 'setWithTypeParameterRemovedTATypeParameterHolder2' can not be called: Function uses unlinked class symbol '/WithTypeParameter' (via data class 'WithTypeParameterRemovedTATypeParameterHolder')")
    ) { setWithTypeParameterRemovedTATypeParameterHolder2(null) }

    expectFailure(
        linkage("Function 'getWithTypeParameterRemovedTAImplTypeParameterHolder' can not be called: Function uses unlinked class symbol '/WithTypeParameter' (via data class 'WithTypeParameterRemovedTAImplTypeParameterHolder')")
    ) { getWithTypeParameterRemovedTAImplTypeParameterHolder(5).toString() }

    expectFailure(
        linkage("Function 'setWithTypeParameterRemovedTAImplTypeParameterHolder' can not be called: Function uses unlinked class symbol '/WithTypeParameter' (via data class 'WithTypeParameterRemovedTAImplTypeParameterHolder')")
    ) { setWithTypeParameterRemovedTAImplTypeParameterHolder(null) }

    // nested
    expectFailure(
        linkage("Function 'getNestedRemovedTA' can not be called: Function uses unlinked class symbol '/Outer.Nested'")
    ) { getNestedRemovedTA(1).toString() }

    expectFailure(
        linkage("Function 'setNestedRemovedTA' can not be called: Function uses unlinked class symbol '/Outer.Nested'")
    ) { setNestedRemovedTA(null) }

    expectFailure(
        linkage("Function 'getNestedRemovedTAImpl' can not be called: Function uses unlinked class symbol '/Outer.Nested'")
    ) { getNestedRemovedTAImpl(2).toString() }

    expectFailure(
        linkage("Function 'setNestedRemovedTAImpl' can not be called: Function uses unlinked class symbol '/Outer.Nested' (via class 'NestedRemovedTAImpl')")
    ) { setNestedRemovedTAImpl(null) }

    expectFailure(
        linkage("Function 'getNestedRemovedTATypeParameterHolder1' can not be called: Function uses unlinked class symbol '/Outer.Nested' (via data class 'NestedRemovedTATypeParameterHolder')")
    ) { getNestedRemovedTATypeParameterHolder1(3).toString() }

    expectFailure(
        linkage("Function 'getNestedRemovedTATypeParameterHolder2' can not be called: Function uses unlinked class symbol '/Outer.Nested' (via data class 'NestedRemovedTATypeParameterHolder')")
    ) { getNestedRemovedTATypeParameterHolder2(4).toString() }

    expectFailure(
        linkage("Function 'setNestedRemovedTATypeParameterHolder1' can not be called: Function uses unlinked class symbol '/Outer.Nested' (via data class 'NestedRemovedTATypeParameterHolder')")
    ) { setNestedRemovedTATypeParameterHolder1(null) }

    expectFailure(
        linkage("Function 'setNestedRemovedTATypeParameterHolder2' can not be called: Function uses unlinked class symbol '/Outer.Nested' (via data class 'NestedRemovedTATypeParameterHolder')")
    ) { setNestedRemovedTATypeParameterHolder2(null) }

    expectFailure(
        linkage("Function 'getNestedRemovedTAImplTypeParameterHolder' can not be called: Function uses unlinked class symbol '/Outer.Nested' (via data class 'NestedRemovedTAImplTypeParameterHolder')")
    ) { getNestedRemovedTAImplTypeParameterHolder(5).toString() }

    expectFailure(
        linkage("Function 'setNestedRemovedTAImplTypeParameterHolder' can not be called: Function uses unlinked class symbol '/Outer.Nested' (via data class 'NestedRemovedTAImplTypeParameterHolder')")
    ) { setNestedRemovedTAImplTypeParameterHolder(null) }
}
