// DIAGNOSTICS: -MISPLACED_TYPE_PARAMETER_CONSTRAINTS, -NOTHING_TO_INLINE, -NO_TAIL_CALLS_FOUND


class MemberScope {


    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) fun testBasic() {}
    fun testBasic() {}


    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) fun testIdenticalReturnTypes(): UserKlass = UserKlass()
    fun testIdenticalReturnTypes(): UserKlass = UserKlass()

    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) fun testDifferencesInReturnTypePresence(): Unit {}
    fun testDifferencesInReturnTypePresence() {}

    fun testDifferencesInReturnTypePresenceReverse(): Unit {}
    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) fun testDifferencesInReturnTypePresenceReverse() {}

    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) fun testDifferentReturnTypes(): UserKlassA = UserKlassA()
    fun testDifferentReturnTypes(): UserKlassB = UserKlassB()

    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) fun testVarianceDifferentReturnTypesA(): Invariant<UserKlass> = Invariant()
    fun testVarianceDifferentReturnTypesA(): Invariant<out UserKlass> = Invariant()

    fun testVarianceDifferentReturnTypesAReverse(): Invariant<UserKlass> = Invariant()
    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) fun testVarianceDifferentReturnTypesAReverse(): Invariant<out UserKlass> = Invariant()

    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) fun testVarianceDifferentReturnTypesB(): Invariant<UserKlass> = Invariant()
    fun testVarianceDifferentReturnTypesB(): Invariant<in UserKlass> = Invariant()

    fun testVarianceDifferentReturnTypesBReverse(): Invariant<UserKlass> = Invariant()
    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) fun testVarianceDifferentReturnTypesBReverse(): Invariant<in UserKlass> = Invariant()

    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) fun testVarianceDifferentReturnTypesC(): Invariant<UserKlass> = Invariant()
    fun testVarianceDifferentReturnTypesC(): Invariant<*> = Invariant<UserKlass>()

    fun testVarianceDifferentReturnTypesCReverse(): Invariant<UserKlass> = Invariant()
    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) fun testVarianceDifferentReturnTypesCReverse(): Invariant<*> = Invariant<UserKlass>()

    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) fun testVarianceDifferentReturnTypesD(): Invariant<out UserKlass> = Invariant()
    fun testVarianceDifferentReturnTypesD(): Invariant<*> = Invariant<UserKlass>()

    fun testVarianceDifferentReturnTypesDReverse(): Invariant<out UserKlass> = Invariant()
    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) fun testVarianceDifferentReturnTypesDReverse(): Invariant<*> = Invariant<UserKlass>()

    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) fun testVarianceDifferentReturnTypesE(): Invariant<in UserKlass> = Invariant()
    fun testVarianceDifferentReturnTypesE(): Invariant<*> = Invariant<UserKlass>()

    fun testVarianceDifferentReturnTypesEReverse(): Invariant<in UserKlass> = Invariant()
    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) fun testVarianceDifferentReturnTypesEReverse(): Invariant<*> = Invariant<UserKlass>()

    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) fun testVarianceDifferentReturnTypesF(): Invariant<out UserKlass> = Invariant()
    fun testVarianceDifferentReturnTypesF(): Invariant<in UserKlass> = Invariant()

    fun testVarianceDifferentReturnTypesFReverse(): Invariant<out UserKlass> = Invariant()
    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) fun testVarianceDifferentReturnTypesFReverse(): Invariant<in UserKlass> = Invariant()

    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) fun testTypeAliasedReturnTypes(): UserKlass = UserKlass()
    fun testTypeAliasedReturnTypes(): SameUserKlass = UserKlass()

    fun testTypeAliasedReturnTypesReverse(): UserKlass = UserKlass()
    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) fun testTypeAliasedReturnTypesReverse(): SameUserKlass = UserKlass()


    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) fun UserKlass.testIdenticalExtensionReceivers() {}
    fun UserKlass.testIdenticalExtensionReceivers() {}

    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) fun UserKlass.testTypeAliasedExtensionReceivers() {}
    fun SameUserKlass.testTypeAliasedExtensionReceivers() {}

    fun UserKlass.testTypeAliasedExtensionReceiversReverse() {}
    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) fun SameUserKlass.testTypeAliasedExtensionReceiversReverse() {}


    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) fun testIdenticalValueParameters(arg: UserKlass) {}
    fun testIdenticalValueParameters(arg: UserKlass) {}

    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) fun testDifferentlyNamedValueParameters(argA: UserKlass) {}
    fun testDifferentlyNamedValueParameters(argB: UserKlass) {}

    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) fun testTypeAliasedValueParameterTypes(arg: UserKlass) {}
    fun testTypeAliasedValueParameterTypes(arg: SameUserKlass) {}

    fun testTypeAliasedValueParameterTypesReverse(arg: UserKlass) {}
    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) fun testTypeAliasedValueParameterTypesReverse(arg: SameUserKlass) {}


    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) fun testMultipleIdenticalValueParameters(arg1: UserKlassA, arg2: UserKlassB) {}
    fun testMultipleIdenticalValueParameters(arg1: UserKlassA, arg2: UserKlassB) {}

    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) fun testMultipleDifferentlyNamedValueParametersA(arg1: UserKlassA, arg2A: UserKlassB) {}
    fun testMultipleDifferentlyNamedValueParametersA(arg1: UserKlassA, arg2B: UserKlassB) {}

    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) fun testMultipleDifferentlyNamedValueParametersB(arg1A: UserKlassA, arg2A: UserKlassB) {}
    fun testMultipleDifferentlyNamedValueParametersB(arg1B: UserKlassA, arg2B: UserKlassB) {}

    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) fun testMultipleTypeAliasedValueParameterTypesA(arg1: UserKlassA, arg2: UserKlassB) {}
    fun testMultipleTypeAliasedValueParameterTypesA(arg1: UserKlassA, arg2: SameUserKlassB) {}

    fun testMultipleTypeAliasedValueParameterTypesAReverse(arg1: UserKlassA, arg2: UserKlassB) {}
    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) fun testMultipleTypeAliasedValueParameterTypesAReverse(arg1: UserKlassA, arg2: SameUserKlassB) {}

    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) fun testMultipleTypeAliasedValueParameterTypesB(arg1: UserKlassA, arg2: UserKlassB) {}
    fun testMultipleTypeAliasedValueParameterTypesB(arg1: SameUserKlassA, arg2: SameUserKlassB) {}

    fun testMultipleTypeAliasedValueParameterTypesBReverse(arg1: UserKlassA, arg2: UserKlassB) {}
    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) fun testMultipleTypeAliasedValueParameterTypesBReverse(arg1: SameUserKlassA, arg2: SameUserKlassB) {}


    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) fun testValueParameterWithIdenticalDefaultArguments(arg: UserKlass = defaultArgument) {}
    fun testValueParameterWithIdenticalDefaultArguments(arg: UserKlass = defaultArgument) {}

    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) fun testDifferencesInValueParameterDefaultArgumentsPresence(arg: UserKlass = defaultArgument) {}
    fun testDifferencesInValueParameterDefaultArgumentsPresence(arg: UserKlass) {}

    fun testDifferencesInValueParameterDefaultArgumentsPresenceReverse(arg: UserKlass = defaultArgument) {}
    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) fun testDifferencesInValueParameterDefaultArgumentsPresenceReverse(arg: UserKlass) {}

    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) fun testValueParameterWithDifferentDefaultArguments(arg: UserKlass = defaultArgumentA) {}
    fun testValueParameterWithDifferentDefaultArguments(arg: UserKlass = defaultArgumentB) {}

    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) fun testValueParameterWithAliasedDefaultArguments(arg: UserKlass = defaultArgument) {}
    fun testValueParameterWithAliasedDefaultArguments(arg: UserKlass = sameDefaultArgument) {}

    fun testValueParameterWithAliasedDefaultArgumentsReverse(arg: UserKlass = defaultArgument) {}
    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) fun testValueParameterWithAliasedDefaultArgumentsReverse(arg: UserKlass = sameDefaultArgument) {}


    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) fun <T> testIdenticalTypeParametersA() {}
    fun <T> testIdenticalTypeParametersA() {}

    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) fun <T> testIdenticalTypeParametersB(arg: T) {}
    fun <T> testIdenticalTypeParametersB(arg: T) {}

    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) fun <T> testIdenticalTypeParametersC(arg: Invariant<T>) {}
    fun <T> testIdenticalTypeParametersC(arg: Invariant<T>) {}

    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) fun <TA> testDifferentlyNamedTypeParametersA() {}
    fun <TB> testDifferentlyNamedTypeParametersA() {}

    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) fun <TA> testDifferentlyNamedTypeParametersB(arg: TA) {}
    fun <TB> testDifferentlyNamedTypeParametersB(arg: TB) {}

    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) fun <TA> testDifferentlyNamedTypeParametersC(arg: Invariant<TA>) {}
    fun <TB> testDifferentlyNamedTypeParametersC(arg: Invariant<TB>) {}


    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) fun <T1, T2> testMultipleIdenticalTypeParameters() {}
    fun <T1, T2> testMultipleIdenticalTypeParameters() {}

    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) fun <T1, T2A> testMultipleDifferentlyNamedTypeParametersA() {}
    fun <T1, T2B> testMultipleDifferentlyNamedTypeParametersA() {}

    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) fun <T1A, T2A> testMultipleDifferentlyNamedTypeParametersB() {}
    fun <T1B, T2B> testMultipleDifferentlyNamedTypeParametersB() {}


    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) fun <T: UserInterface> testTypeParameterWithIdenticalUpperBoundsA() {}
    fun <T: UserInterface> testTypeParameterWithIdenticalUpperBoundsA() {}

    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) fun <T: UserInterface> testTypeParameterWithIdenticalUpperBoundsB(arg: T) {}
    fun <T: UserInterface> testTypeParameterWithIdenticalUpperBoundsB(arg: T) {}

    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) fun <T: UserInterface> testTypeParameterWithIdenticalUpperBoundsC(arg: Invariant<T>) {}
    fun <T: UserInterface> testTypeParameterWithIdenticalUpperBoundsC(arg: Invariant<T>) {}

    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) fun <T: UserInterface> testDifferencesInTypeParameterUpperBoundsPresence() {}
    fun <T> testDifferencesInTypeParameterUpperBoundsPresence() {}

    fun <T: UserInterface> testDifferencesInTypeParameterUpperBoundsPresenceReverse() {}
    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) fun <T> testDifferencesInTypeParameterUpperBoundsPresenceReverse() {}

    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) fun <T: UserInterfaceA> testTypeParameterWithDifferentUpperBounds() {}
    fun <T: UserInterfaceB> testTypeParameterWithDifferentUpperBounds() {}

    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) fun <T: Invariant<UserInterface>> testTypeParameterWithVarianceDifferentUpperBoundsA() {}
    fun <T: Invariant<out UserInterface>> testTypeParameterWithVarianceDifferentUpperBoundsA() {}

    fun <T: Invariant<UserInterface>> testTypeParameterWithVarianceDifferentUpperBoundsAReverse() {}
    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) fun <T: Invariant<out UserInterface>> testTypeParameterWithVarianceDifferentUpperBoundsAReverse() {}

    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) fun <T: Invariant<UserInterface>> testTypeParameterWithVarianceDifferentUpperBoundsB() {}
    fun <T: Invariant<in UserInterface>> testTypeParameterWithVarianceDifferentUpperBoundsB() {}

    fun <T: Invariant<UserInterface>> testTypeParameterWithVarianceDifferentUpperBoundsBReverse() {}
    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) fun <T: Invariant<in UserInterface>> testTypeParameterWithVarianceDifferentUpperBoundsBReverse() {}

    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) fun <T: Invariant<UserInterface>> testTypeParameterWithVarianceDifferentUpperBoundsC() {}
    fun <T: Invariant<*>> testTypeParameterWithVarianceDifferentUpperBoundsC() {}

    fun <T: Invariant<UserInterface>> testTypeParameterWithVarianceDifferentUpperBoundsCReverse() {}
    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) fun <T: Invariant<*>> testTypeParameterWithVarianceDifferentUpperBoundsCReverse() {}

    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) fun <T: Invariant<out UserInterface>> testTypeParameterWithVarianceDifferentUpperBoundsD() {}
    fun <T: Invariant<*>> testTypeParameterWithVarianceDifferentUpperBoundsD() {}

    fun <T: Invariant<out UserInterface>> testTypeParameterWithVarianceDifferentUpperBoundsDReverse() {}
    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) fun <T: Invariant<*>> testTypeParameterWithVarianceDifferentUpperBoundsDReverse() {}

    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) fun <T: Invariant<in UserInterface>> testTypeParameterWithVarianceDifferentUpperBoundsE() {}
    fun <T: Invariant<*>> testTypeParameterWithVarianceDifferentUpperBoundsE() {}

    fun <T: Invariant<in UserInterface>> testTypeParameterWithVarianceDifferentUpperBoundsEReverse() {}
    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) fun <T: Invariant<*>> testTypeParameterWithVarianceDifferentUpperBoundsEReverse() {}

    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) fun <T: Invariant<out UserInterface>> testTypeParameterWithVarianceDifferentUpperBoundsF() {}
    fun <T: Invariant<in UserInterface>> testTypeParameterWithVarianceDifferentUpperBoundsF() {}

    fun <T: Invariant<out UserInterface>> testTypeParameterWithVarianceDifferentUpperBoundsFReverse() {}
    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) fun <T: Invariant<in UserInterface>> testTypeParameterWithVarianceDifferentUpperBoundsFReverse() {}

    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) fun <T: UserInterface> testTypeParameterWithTypeAliasedUpperBoundsA() {}
    fun <T: SameUserInterface> testTypeParameterWithTypeAliasedUpperBoundsA() {}

    fun <T: UserInterface> testTypeParameterWithTypeAliasedUpperBoundsAReverse() {}
    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) fun <T: SameUserInterface> testTypeParameterWithTypeAliasedUpperBoundsAReverse() {}

    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) fun <T: UserInterface> testTypeParameterWithTypeAliasedUpperBoundsB(arg: T) {}
    fun <T: SameUserInterface> testTypeParameterWithTypeAliasedUpperBoundsB(arg: T) {}

    fun <T: UserInterface> testTypeParameterWithTypeAliasedUpperBoundsBReverse(arg: T) {}
    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) fun <T: SameUserInterface> testTypeParameterWithTypeAliasedUpperBoundsBReverse(arg: T) {}

    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) fun <T: UserInterface> testTypeParameterWithTypeAliasedUpperBoundsC(arg: Invariant<T>) {}
    fun <T: SameUserInterface> testTypeParameterWithTypeAliasedUpperBoundsC(arg: Invariant<T>) {}

    fun <T: UserInterface> testTypeParameterWithTypeAliasedUpperBoundsCReverse(arg: Invariant<T>) {}
    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) fun <T: SameUserInterface> testTypeParameterWithTypeAliasedUpperBoundsCReverse(arg: Invariant<T>) {}


    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) fun <T> testTypeParameterWithMultipleIdenticalUpperBoundsAA() where T: UserInterfaceA, T: UserInterfaceB {}
    fun <T> testTypeParameterWithMultipleIdenticalUpperBoundsAA() where T: UserInterfaceA, T: UserInterfaceB {}

    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) fun <T> testTypeParameterWithMultipleIdenticalUpperBoundsAB(arg: T) where T: UserInterfaceA, T: UserInterfaceB {}
    fun <T> testTypeParameterWithMultipleIdenticalUpperBoundsAB(arg: T) where T: UserInterfaceA, T: UserInterfaceB {}

    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) fun <T> testTypeParameterWithMultipleIdenticalUpperBoundsAC(arg: Invariant<T>) where T: UserInterfaceA, T: UserInterfaceB {}
    fun <T> testTypeParameterWithMultipleIdenticalUpperBoundsAC(arg: Invariant<T>) where T: UserInterfaceA, T: UserInterfaceB {}

    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) fun <T: UserInterfaceA> testTypeParameterWithMultipleIdenticalUpperBoundsBA() where T: UserInterfaceB {}
    fun <T: UserInterfaceA> testTypeParameterWithMultipleIdenticalUpperBoundsBA() where T: UserInterfaceB {}

    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) fun <T: UserInterfaceA> testTypeParameterWithMultipleIdenticalUpperBoundsBB(arg: T) where T: UserInterfaceB {}
    fun <T: UserInterfaceA> testTypeParameterWithMultipleIdenticalUpperBoundsBB(arg: T) where T: UserInterfaceB {}

    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) fun <T: UserInterfaceA> testTypeParameterWithMultipleIdenticalUpperBoundsBC(arg: Invariant<T>) where T: UserInterfaceB {}
    fun <T: UserInterfaceA> testTypeParameterWithMultipleIdenticalUpperBoundsBC(arg: Invariant<T>) where T: UserInterfaceB {}

    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) fun <T> testDifferencesInTypeParameterMultipleUpperBoundsPresenceA() where T: UserInterfaceA, T: UserInterfaceB {}
    fun <T> testDifferencesInTypeParameterMultipleUpperBoundsPresenceA() where T: UserInterfaceA {}

    fun <T: UserInterfaceA> testDifferencesInTypeParameterMultipleUpperBoundsPresenceB() where T: UserInterfaceB {}
    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) fun <T: UserInterfaceA> testDifferencesInTypeParameterMultipleUpperBoundsPresenceB() {}

    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) fun <T> testTypeParameterWithMultipleDifferentUpperBoundsAA() where T: UserInterfaceA, T: UserInterfaceB {}
    fun <T> testTypeParameterWithMultipleDifferentUpperBoundsAA() where T: UserInterfaceA, T: UserInterfaceC {}

    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) fun <T: UserInterfaceA> testTypeParameterWithMultipleDifferentUpperBoundsAB() where T: UserInterfaceB {}
    fun <T: UserInterfaceA> testTypeParameterWithMultipleDifferentUpperBoundsAB() where T: UserInterfaceC {}

    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) fun <T: UserInterfaceB> testTypeParameterWithMultipleDifferentUpperBoundsAC() where T: UserInterfaceA {}
    fun <T: UserInterfaceC> testTypeParameterWithMultipleDifferentUpperBoundsAC() where T: UserInterfaceA {}

    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) fun <T> testTypeParameterWithMultipleDifferentUpperBoundsBA() where T: UserInterfaceA, T: UserInterfaceB {}
    fun <T> testTypeParameterWithMultipleDifferentUpperBoundsBA() where T: UserInterfaceC, T: UserInterfaceD {}

    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) fun <T: UserInterfaceA> testTypeParameterWithMultipleDifferentUpperBoundsBB() where T: UserInterfaceB {}
    fun <T: UserInterfaceC> testTypeParameterWithMultipleDifferentUpperBoundsBB() where T: UserInterfaceD {}

    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) fun <T> testTypeParameterWithMultipleTypeAliasedUpperBoundsAAA() where T: UserInterfaceA, T: UserInterfaceB {}
    fun <T> testTypeParameterWithMultipleTypeAliasedUpperBoundsAAA() where T: UserInterfaceA, T: SameUserInterfaceB {}

    fun <T> testTypeParameterWithMultipleTypeAliasedUpperBoundsAAAReverse() where T: UserInterfaceA, T: UserInterfaceB {}
    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) fun <T> testTypeParameterWithMultipleTypeAliasedUpperBoundsAAAReverse() where T: UserInterfaceA, T: SameUserInterfaceB {}

    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) fun <T> testTypeParameterWithMultipleTypeAliasedUpperBoundsAAB(arg: T) where T: UserInterfaceA, T: UserInterfaceB {}
    fun <T> testTypeParameterWithMultipleTypeAliasedUpperBoundsAAB(arg: T) where T: UserInterfaceA, T: SameUserInterfaceB {}

    fun <T> testTypeParameterWithMultipleTypeAliasedUpperBoundsAABReverse(arg: T) where T: UserInterfaceA, T: UserInterfaceB {}
    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) fun <T> testTypeParameterWithMultipleTypeAliasedUpperBoundsAABReverse(arg: T) where T: UserInterfaceA, T: SameUserInterfaceB {}

    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) fun <T> testTypeParameterWithMultipleTypeAliasedUpperBoundsAAC(arg: Invariant<T>) where T: UserInterfaceA, T: UserInterfaceB {}
    fun <T> testTypeParameterWithMultipleTypeAliasedUpperBoundsAAC(arg: Invariant<T>) where T: UserInterfaceA, T: SameUserInterfaceB {}

    fun <T> testTypeParameterWithMultipleTypeAliasedUpperBoundsAACReverse(arg: Invariant<T>) where T: UserInterfaceA, T: UserInterfaceB {}
    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) fun <T> testTypeParameterWithMultipleTypeAliasedUpperBoundsAACReverse(arg: Invariant<T>) where T: UserInterfaceA, T: SameUserInterfaceB {}

    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) fun <T: UserInterfaceA> testTypeParameterWithMultipleTypeAliasedUpperBoundsABA() where T: UserInterfaceB {}
    fun <T: UserInterfaceA> testTypeParameterWithMultipleTypeAliasedUpperBoundsABA() where T: SameUserInterfaceB {}

    fun <T: UserInterfaceA> testTypeParameterWithMultipleTypeAliasedUpperBoundsABAReverse() where T: UserInterfaceB {}
    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) fun <T: UserInterfaceA> testTypeParameterWithMultipleTypeAliasedUpperBoundsABAReverse() where T: SameUserInterfaceB {}

    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) fun <T: UserInterfaceA> testTypeParameterWithMultipleTypeAliasedUpperBoundsABB(arg: T) where T: UserInterfaceB {}
    fun <T: UserInterfaceA> testTypeParameterWithMultipleTypeAliasedUpperBoundsABB(arg: T) where T: SameUserInterfaceB {}

    fun <T: UserInterfaceA> testTypeParameterWithMultipleTypeAliasedUpperBoundsABBReverse(arg: T) where T: UserInterfaceB {}
    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) fun <T: UserInterfaceA> testTypeParameterWithMultipleTypeAliasedUpperBoundsABBReverse(arg: T) where T: SameUserInterfaceB {}

    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) fun <T: UserInterfaceA> testTypeParameterWithMultipleTypeAliasedUpperBoundsABC(arg: Invariant<T>) where T: UserInterfaceB {}
    fun <T: UserInterfaceA> testTypeParameterWithMultipleTypeAliasedUpperBoundsABC(arg: Invariant<T>) where T: SameUserInterfaceB {}

    fun <T: UserInterfaceA> testTypeParameterWithMultipleTypeAliasedUpperBoundsABCReverse(arg: Invariant<T>) where T: UserInterfaceB {}
    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) fun <T: UserInterfaceA> testTypeParameterWithMultipleTypeAliasedUpperBoundsABCReverse(arg: Invariant<T>) where T: SameUserInterfaceB {}

    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) fun <T: UserInterfaceB> testTypeParameterWithMultipleTypeAliasedUpperBoundsACA() where T: UserInterfaceA {}
    fun <T: SameUserInterfaceB> testTypeParameterWithMultipleTypeAliasedUpperBoundsACA() where T: UserInterfaceA {}

    fun <T: UserInterfaceB> testTypeParameterWithMultipleTypeAliasedUpperBoundsACAReverse() where T: UserInterfaceA {}
    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) fun <T: SameUserInterfaceB> testTypeParameterWithMultipleTypeAliasedUpperBoundsACAReverse() where T: UserInterfaceA {}

    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) fun <T: UserInterfaceB> testTypeParameterWithMultipleTypeAliasedUpperBoundsACB(arg: T) where T: UserInterfaceA {}
    fun <T: SameUserInterfaceB> testTypeParameterWithMultipleTypeAliasedUpperBoundsACB(arg: T) where T: UserInterfaceA {}

    fun <T: UserInterfaceB> testTypeParameterWithMultipleTypeAliasedUpperBoundsACBReverse(arg: T) where T: UserInterfaceA {}
    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) fun <T: SameUserInterfaceB> testTypeParameterWithMultipleTypeAliasedUpperBoundsACBReverse(arg: T) where T: UserInterfaceA {}

    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) fun <T: UserInterfaceB> testTypeParameterWithMultipleTypeAliasedUpperBoundsACC(arg: Invariant<T>) where T: UserInterfaceA {}
    fun <T: SameUserInterfaceB> testTypeParameterWithMultipleTypeAliasedUpperBoundsACC(arg: Invariant<T>) where T: UserInterfaceA {}

    fun <T: UserInterfaceB> testTypeParameterWithMultipleTypeAliasedUpperBoundsACCReverse(arg: Invariant<T>) where T: UserInterfaceA {}
    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) fun <T: SameUserInterfaceB> testTypeParameterWithMultipleTypeAliasedUpperBoundsACCReverse(arg: Invariant<T>) where T: UserInterfaceA {}

    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) fun <T> testTypeParameterWithMultipleTypeAliasedUpperBoundsBAA() where T: UserInterfaceA, T: UserInterfaceB {}
    fun <T> testTypeParameterWithMultipleTypeAliasedUpperBoundsBAA() where T: SameUserInterfaceA, T: SameUserInterfaceB {}

    fun <T> testTypeParameterWithMultipleTypeAliasedUpperBoundsBAAReverse() where T: UserInterfaceA, T: UserInterfaceB {}
    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) fun <T> testTypeParameterWithMultipleTypeAliasedUpperBoundsBAAReverse() where T: SameUserInterfaceA, T: SameUserInterfaceB {}

    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) fun <T> testTypeParameterWithMultipleTypeAliasedUpperBoundsBAB(arg: T) where T: UserInterfaceA, T: UserInterfaceB {}
    fun <T> testTypeParameterWithMultipleTypeAliasedUpperBoundsBAB(arg: T) where T: SameUserInterfaceA, T: SameUserInterfaceB {}

    fun <T> testTypeParameterWithMultipleTypeAliasedUpperBoundsBABReverse(arg: T) where T: UserInterfaceA, T: UserInterfaceB {}
    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) fun <T> testTypeParameterWithMultipleTypeAliasedUpperBoundsBABReverse(arg: T) where T: SameUserInterfaceA, T: SameUserInterfaceB {}

    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) fun <T> testTypeParameterWithMultipleTypeAliasedUpperBoundsBAC(arg: Invariant<T>) where T: UserInterfaceA, T: UserInterfaceB {}
    fun <T> testTypeParameterWithMultipleTypeAliasedUpperBoundsBAC(arg: Invariant<T>) where T: SameUserInterfaceA, T: SameUserInterfaceB {}

    fun <T> testTypeParameterWithMultipleTypeAliasedUpperBoundsBACReverse(arg: Invariant<T>) where T: UserInterfaceA, T: UserInterfaceB {}
    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) fun <T> testTypeParameterWithMultipleTypeAliasedUpperBoundsBACReverse(arg: Invariant<T>) where T: SameUserInterfaceA, T: SameUserInterfaceB {}

    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) fun <T: UserInterfaceA> testTypeParameterWithMultipleTypeAliasedUpperBoundsBBA() where T: UserInterfaceB {}
    fun <T: SameUserInterfaceA> testTypeParameterWithMultipleTypeAliasedUpperBoundsBBA() where T: SameUserInterfaceB {}

    fun <T: UserInterfaceA> testTypeParameterWithMultipleTypeAliasedUpperBoundsBBAReverse() where T: UserInterfaceB {}
    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) fun <T: SameUserInterfaceA> testTypeParameterWithMultipleTypeAliasedUpperBoundsBBAReverse() where T: SameUserInterfaceB {}

    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) fun <T: UserInterfaceA> testTypeParameterWithMultipleTypeAliasedUpperBoundsBBB(arg: T) where T: UserInterfaceB {}
    fun <T: SameUserInterfaceA> testTypeParameterWithMultipleTypeAliasedUpperBoundsBBB(arg: T) where T: SameUserInterfaceB {}

    fun <T: UserInterfaceA> testTypeParameterWithMultipleTypeAliasedUpperBoundsBBBReverse(arg: T) where T: UserInterfaceB {}
    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) fun <T: SameUserInterfaceA> testTypeParameterWithMultipleTypeAliasedUpperBoundsBBBReverse(arg: T) where T: SameUserInterfaceB {}

    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) fun <T: UserInterfaceA> testTypeParameterWithMultipleTypeAliasedUpperBoundsBBC(arg: Invariant<T>) where T: UserInterfaceB {}
    fun <T: SameUserInterfaceA> testTypeParameterWithMultipleTypeAliasedUpperBoundsBBC(arg: Invariant<T>) where T: SameUserInterfaceB {}

    fun <T: UserInterfaceA> testTypeParameterWithMultipleTypeAliasedUpperBoundsBBCReverse(arg: Invariant<T>) where T: UserInterfaceB {}
    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) fun <T: SameUserInterfaceA> testTypeParameterWithMultipleTypeAliasedUpperBoundsBBCReverse(arg: Invariant<T>) where T: SameUserInterfaceB {}

    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) fun <T> testTypeParameterWithMultipleShuffledUpperBoundsAA() where T: UserInterfaceA, T: UserInterfaceB {}
    fun <T> testTypeParameterWithMultipleShuffledUpperBoundsAA() where T: UserInterfaceB, T: UserInterfaceA {}

    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) fun <T> testTypeParameterWithMultipleShuffledUpperBoundsAB(arg: T) where T: UserInterfaceA, T: UserInterfaceB {}
    fun <T> testTypeParameterWithMultipleShuffledUpperBoundsAB(arg: T) where T: UserInterfaceB, T: UserInterfaceA {}

    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) fun <T> testTypeParameterWithMultipleShuffledUpperBoundsAC(arg: Invariant<T>) where T: UserInterfaceA, T: UserInterfaceB {}
    fun <T> testTypeParameterWithMultipleShuffledUpperBoundsAC(arg: Invariant<T>) where T: UserInterfaceB, T: UserInterfaceA {}

    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) fun <T: UserInterfaceA> testTypeParameterWithMultipleShuffledUpperBoundsBA() where T: UserInterfaceB {}
    fun <T: UserInterfaceB> testTypeParameterWithMultipleShuffledUpperBoundsBA() where T: UserInterfaceA {}

    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) fun <T: UserInterfaceA> testTypeParameterWithMultipleShuffledUpperBoundsBB(arg: T) where T: UserInterfaceB {}
    fun <T: UserInterfaceB> testTypeParameterWithMultipleShuffledUpperBoundsBB(arg: T) where T: UserInterfaceA {}

    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) fun <T: UserInterfaceA> testTypeParameterWithMultipleShuffledUpperBoundsBC(arg: Invariant<T>) where T: UserInterfaceB {}
    fun <T: UserInterfaceB> testTypeParameterWithMultipleShuffledUpperBoundsBC(arg: Invariant<T>) where T: UserInterfaceA {}


    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) inline fun <reified T> testIdenticalReifiedTypeParameterA() {}
    inline fun <reified T> testIdenticalReifiedTypeParameterA() {}

    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) inline fun <reified T> testIdenticalReifiedTypeParameterB(arg: T) {}
    inline fun <reified T> testIdenticalReifiedTypeParameterB(arg: T) {}

    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) inline fun <reified T> testIdenticalReifiedTypeParameterC(arg: Invariant<T>) {}
    inline fun <reified T> testIdenticalReifiedTypeParameterC(arg: Invariant<T>) {}

    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) inline fun <reified T> testDifferencesInReifiedBehaviorOfTypeParameterA() {}
    inline fun <T> testDifferencesInReifiedBehaviorOfTypeParameterA() {}

    inline fun <reified T> testDifferencesInReifiedBehaviorOfTypeParameterAReverse() {}
    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) inline fun <T> testDifferencesInReifiedBehaviorOfTypeParameterAReverse() {}

    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) inline fun <reified T> testDifferencesInReifiedBehaviorOfTypeParameterB(arg: T) {}
    inline fun <T> testDifferencesInReifiedBehaviorOfTypeParameterB(arg: T) {}

    inline fun <reified T> testDifferencesInReifiedBehaviorOfTypeParameterBReverse(arg: T) {}
    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) inline fun <T> testDifferencesInReifiedBehaviorOfTypeParameterBReverse(arg: T) {}

    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) inline fun <reified T> testDifferencesInReifiedBehaviorOfTypeParameterC(arg: Invariant<T>) {}
    inline fun <T> testDifferencesInReifiedBehaviorOfTypeParameterC(arg: Invariant<T>) {}

    inline fun <reified T> testDifferencesInReifiedBehaviorOfTypeParameterCReverse(arg: Invariant<T>) {}
    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) inline fun <T> testDifferencesInReifiedBehaviorOfTypeParameterCReverse(arg: Invariant<T>) {}


    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) inline fun testIdenticalPresenceOfInlineModifier() {}
    inline fun testIdenticalPresenceOfInlineModifier() {}

    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) inline fun testDifferencesInInlineModifierPresence() {}
    fun testDifferencesInInlineModifierPresence() {}

    inline fun testDifferencesInInlineModifierPresenceReverse() {}
    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) fun testDifferencesInInlineModifierPresenceReverse() {}


    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) infix fun UserKlass.testIdenticalPresenceOfInfixModifier(arg: UserKlass) {}
    infix fun UserKlass.testIdenticalPresenceOfInfixModifier(arg: UserKlass) {}

    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) infix fun UserKlass.testDifferencesInInfixModifierPresence(arg: UserKlass) {}
    fun UserKlass.testDifferencesInInfixModifierPresence(arg: UserKlass) {}

    infix fun UserKlass.testDifferencesInInfixModifierPresenceReverse(arg: UserKlass) {}
    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) fun UserKlass.testDifferencesInInfixModifierPresenceReverse(arg: UserKlass) {}


    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) operator fun UserKlassA.unaryPlus() {}
    operator fun UserKlassA.unaryPlus() {}

    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) operator fun UserKlassB.unaryPlus() {}
    fun UserKlassB.unaryPlus() {}

    operator fun UserKlassB.unaryMinus() {}
    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) fun UserKlassB.unaryMinus() {}


    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) tailrec fun testIdenticalPresenceOfTailrecModifier() {}
    tailrec fun testIdenticalPresenceOfTailrecModifier() {}

    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) tailrec fun testDifferencesInTailrecModifierPresence() {}
    fun testDifferencesInTailrecModifierPresence() {}

    tailrec fun testDifferencesInTailrecModifierPresenceReverse() {}
    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) fun testDifferencesInTailrecModifierPresenceReverse() {}


    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) private fun testIdenticalPrivateVisibility() {}
    private fun testIdenticalPrivateVisibility() {}

    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) internal fun testIdenticalInternalVisibility() {}
    internal fun testIdenticalInternalVisibility() {}

    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) private fun testDifferencesInPrivateAndPublicVisibilities() {}
    public fun testDifferencesInPrivateAndPublicVisibilities() {}

    private fun testDifferencesInPrivateAndPublicVisibilitiesReverse() {}
    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) public fun testDifferencesInPrivateAndPublicVisibilitiesReverse() {}

    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) internal fun testDifferencesInInternalAndPublicVisibilities() {}
    public fun testDifferencesInInternalAndPublicVisibilities() {}

    internal fun testDifferencesInInternalAndPublicVisibilitiesReverse() {}
    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) public fun testDifferencesInInternalAndPublicVisibilitiesReverse() {}

    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) private fun testDifferencesInPrivateAndInternalVisibilities() {}
    internal fun testDifferencesInPrivateAndInternalVisibilities() {}

    private fun testDifferencesInPrivateAndInternalVisibilitiesReverse() {}
    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) internal fun testDifferencesInPrivateAndInternalVisibilitiesReverse() {}


}


open class Invariant<T>


class UserKlass
class UserKlassA
class UserKlassB
typealias SameUserKlass = UserKlass
typealias SameUserKlassA = UserKlassA
typealias SameUserKlassB = UserKlassB


val defaultArgument = UserKlass()
val defaultArgumentA = UserKlass()
val defaultArgumentB = UserKlass()
val sameDefaultArgument = defaultArgument


interface UserInterface
interface UserInterfaceA
interface UserInterfaceB
interface UserInterfaceC
interface UserInterfaceD
typealias SameUserInterface = UserInterface
typealias SameUserInterfaceA = UserInterfaceA
typealias SameUserInterfaceB = UserInterfaceB
