// DIAGNOSTICS: -MISPLACED_TYPE_PARAMETER_CONSTRAINTS

open class MemberScope {

    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) open <!CONFLICTING_OVERLOADS!>fun testBasic()<!> {}
    open <!CONFLICTING_OVERLOADS!>fun testBasic()<!> {}
    
    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) open <!CONFLICTING_OVERLOADS!>fun testIdenticalReturnTypes(): UserKlass<!> = UserKlass()
    open <!CONFLICTING_OVERLOADS!>fun testIdenticalReturnTypes(): UserKlass<!> = UserKlass()

    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) open <!CONFLICTING_OVERLOADS!>fun testDifferencesInReturnTypePresence(): Unit<!> {}
    open <!CONFLICTING_OVERLOADS!>fun testDifferencesInReturnTypePresence()<!> {}

    open <!CONFLICTING_OVERLOADS!>fun testDifferencesInReturnTypePresenceReverse(): Unit<!> {}
    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) open <!CONFLICTING_OVERLOADS!>fun testDifferencesInReturnTypePresenceReverse()<!> {}

    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) open <!CONFLICTING_OVERLOADS!>fun testDifferentReturnTypes(): UserKlassA<!> = UserKlassA()
    open <!CONFLICTING_OVERLOADS!>fun testDifferentReturnTypes(): UserKlassB<!> = UserKlassB()

    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) open <!CONFLICTING_OVERLOADS!>fun testVarianceDifferentReturnTypesA(): Invariant<UserKlass><!> = Invariant()
    open <!CONFLICTING_OVERLOADS!>fun testVarianceDifferentReturnTypesA(): Invariant<out UserKlass><!> = Invariant()

    open <!CONFLICTING_OVERLOADS!>fun testVarianceDifferentReturnTypesAReverse(): Invariant<UserKlass><!> = Invariant()
    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) open <!CONFLICTING_OVERLOADS!>fun testVarianceDifferentReturnTypesAReverse(): Invariant<out UserKlass><!> = Invariant()

    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) open <!CONFLICTING_OVERLOADS!>fun testVarianceDifferentReturnTypesB(): Invariant<UserKlass><!> = Invariant()
    open <!CONFLICTING_OVERLOADS!>fun testVarianceDifferentReturnTypesB(): Invariant<in UserKlass><!> = Invariant()

    open <!CONFLICTING_OVERLOADS!>fun testVarianceDifferentReturnTypesBReverse(): Invariant<UserKlass><!> = Invariant()
    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) open <!CONFLICTING_OVERLOADS!>fun testVarianceDifferentReturnTypesBReverse(): Invariant<in UserKlass><!> = Invariant()

    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) open <!CONFLICTING_OVERLOADS!>fun testVarianceDifferentReturnTypesC(): Invariant<UserKlass><!> = Invariant()
    open <!CONFLICTING_OVERLOADS!>fun testVarianceDifferentReturnTypesC(): Invariant<*><!> = Invariant<UserKlass>()

    open <!CONFLICTING_OVERLOADS!>fun testVarianceDifferentReturnTypesCReverse(): Invariant<UserKlass><!> = Invariant()
    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) open <!CONFLICTING_OVERLOADS!>fun testVarianceDifferentReturnTypesCReverse(): Invariant<*><!> = Invariant<UserKlass>()

    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) open <!CONFLICTING_OVERLOADS!>fun testVarianceDifferentReturnTypesD(): Invariant<out UserKlass><!> = Invariant()
    open <!CONFLICTING_OVERLOADS!>fun testVarianceDifferentReturnTypesD(): Invariant<*><!> = Invariant<UserKlass>()

    open <!CONFLICTING_OVERLOADS!>fun testVarianceDifferentReturnTypesDReverse(): Invariant<out UserKlass><!> = Invariant()
    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) open <!CONFLICTING_OVERLOADS!>fun testVarianceDifferentReturnTypesDReverse(): Invariant<*><!> = Invariant<UserKlass>()

    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) open <!CONFLICTING_OVERLOADS!>fun testVarianceDifferentReturnTypesE(): Invariant<in UserKlass><!> = Invariant()
    open <!CONFLICTING_OVERLOADS!>fun testVarianceDifferentReturnTypesE(): Invariant<*><!> = Invariant<UserKlass>()

    open <!CONFLICTING_OVERLOADS!>fun testVarianceDifferentReturnTypesEReverse(): Invariant<in UserKlass><!> = Invariant()
    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) open <!CONFLICTING_OVERLOADS!>fun testVarianceDifferentReturnTypesEReverse(): Invariant<*><!> = Invariant<UserKlass>()

    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) open <!CONFLICTING_OVERLOADS!>fun testVarianceDifferentReturnTypesF(): Invariant<out UserKlass><!> = Invariant()
    open <!CONFLICTING_OVERLOADS!>fun testVarianceDifferentReturnTypesF(): Invariant<in UserKlass><!> = Invariant()

    open <!CONFLICTING_OVERLOADS!>fun testVarianceDifferentReturnTypesFReverse(): Invariant<out UserKlass><!> = Invariant()
    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) open <!CONFLICTING_OVERLOADS!>fun testVarianceDifferentReturnTypesFReverse(): Invariant<in UserKlass><!> = Invariant()

    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) open <!CONFLICTING_OVERLOADS!>fun testTypeAliasedReturnTypes(): UserKlass<!> = UserKlass()
    open <!CONFLICTING_OVERLOADS!>fun testTypeAliasedReturnTypes(): SameUserKlass<!> = UserKlass()

    open <!CONFLICTING_OVERLOADS!>fun testTypeAliasedReturnTypesReverse(): UserKlass<!> = UserKlass()
    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) open <!CONFLICTING_OVERLOADS!>fun testTypeAliasedReturnTypesReverse(): SameUserKlass<!> = UserKlass()

    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) open <!CONFLICTING_OVERLOADS!>fun UserKlass.testIdenticalExtensionReceivers()<!> {}
    open <!CONFLICTING_OVERLOADS!>fun UserKlass.testIdenticalExtensionReceivers()<!> {}

    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) open <!CONFLICTING_OVERLOADS!>fun UserKlass.testTypeAliasedExtensionReceivers()<!> {}
    open <!CONFLICTING_OVERLOADS!>fun SameUserKlass.testTypeAliasedExtensionReceivers()<!> {}

    open <!CONFLICTING_OVERLOADS!>fun UserKlass.testTypeAliasedExtensionReceiversReverse()<!> {}
    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) open <!CONFLICTING_OVERLOADS!>fun SameUserKlass.testTypeAliasedExtensionReceiversReverse()<!> {}

    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) open <!CONFLICTING_OVERLOADS!>fun testIdenticalValueParameters(arg: UserKlass)<!> {}
    open <!CONFLICTING_OVERLOADS!>fun testIdenticalValueParameters(arg: UserKlass)<!> {}

    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) open <!CONFLICTING_OVERLOADS!>fun testDifferentlyNamedValueParameters(argA: UserKlass)<!> {}
    open <!CONFLICTING_OVERLOADS!>fun testDifferentlyNamedValueParameters(argB: UserKlass)<!> {}

    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) open <!CONFLICTING_OVERLOADS!>fun testTypeAliasedValueParameterTypes(arg: UserKlass)<!> {}
    open <!CONFLICTING_OVERLOADS!>fun testTypeAliasedValueParameterTypes(arg: SameUserKlass)<!> {}

    open <!CONFLICTING_OVERLOADS!>fun testTypeAliasedValueParameterTypesReverse(arg: UserKlass)<!> {}
    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) open <!CONFLICTING_OVERLOADS!>fun testTypeAliasedValueParameterTypesReverse(arg: SameUserKlass)<!> {}

    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) open <!CONFLICTING_OVERLOADS!>fun testMultipleIdenticalValueParameters(arg1: UserKlassA, arg2: UserKlassB)<!> {}
    open <!CONFLICTING_OVERLOADS!>fun testMultipleIdenticalValueParameters(arg1: UserKlassA, arg2: UserKlassB)<!> {}

    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) open <!CONFLICTING_OVERLOADS!>fun testMultipleDifferentlyNamedValueParametersA(arg1: UserKlassA, arg2A: UserKlassB)<!> {}
    open <!CONFLICTING_OVERLOADS!>fun testMultipleDifferentlyNamedValueParametersA(arg1: UserKlassA, arg2B: UserKlassB)<!> {}

    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) open <!CONFLICTING_OVERLOADS!>fun testMultipleDifferentlyNamedValueParametersB(arg1A: UserKlassA, arg2A: UserKlassB)<!> {}
    open <!CONFLICTING_OVERLOADS!>fun testMultipleDifferentlyNamedValueParametersB(arg1B: UserKlassA, arg2B: UserKlassB)<!> {}

    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) open <!CONFLICTING_OVERLOADS!>fun testMultipleTypeAliasedValueParameterTypesA(arg1: UserKlassA, arg2: UserKlassB)<!> {}
    open <!CONFLICTING_OVERLOADS!>fun testMultipleTypeAliasedValueParameterTypesA(arg1: UserKlassA, arg2: SameUserKlassB)<!> {}

    open <!CONFLICTING_OVERLOADS!>fun testMultipleTypeAliasedValueParameterTypesAReverse(arg1: UserKlassA, arg2: UserKlassB)<!> {}
    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) open <!CONFLICTING_OVERLOADS!>fun testMultipleTypeAliasedValueParameterTypesAReverse(arg1: UserKlassA, arg2: SameUserKlassB)<!> {}

    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) open <!CONFLICTING_OVERLOADS!>fun testMultipleTypeAliasedValueParameterTypesB(arg1: UserKlassA, arg2: UserKlassB)<!> {}
    open <!CONFLICTING_OVERLOADS!>fun testMultipleTypeAliasedValueParameterTypesB(arg1: SameUserKlassA, arg2: SameUserKlassB)<!> {}

    open <!CONFLICTING_OVERLOADS!>fun testMultipleTypeAliasedValueParameterTypesBReverse(arg1: UserKlassA, arg2: UserKlassB)<!> {}
    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) open <!CONFLICTING_OVERLOADS!>fun testMultipleTypeAliasedValueParameterTypesBReverse(arg1: SameUserKlassA, arg2: SameUserKlassB)<!> {}

    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) open <!CONFLICTING_OVERLOADS!>fun testValueParameterWithIdenticalDefaultArguments(arg: UserKlass = defaultArgument)<!> {}
    open <!CONFLICTING_OVERLOADS!>fun testValueParameterWithIdenticalDefaultArguments(arg: UserKlass = defaultArgument)<!> {}

    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) open <!CONFLICTING_OVERLOADS!>fun testDifferencesInValueParameterDefaultArgumentsPresence(arg: UserKlass = defaultArgument)<!> {}
    open <!CONFLICTING_OVERLOADS!>fun testDifferencesInValueParameterDefaultArgumentsPresence(arg: UserKlass)<!> {}

    open <!CONFLICTING_OVERLOADS!>fun testDifferencesInValueParameterDefaultArgumentsPresenceReverse(arg: UserKlass = defaultArgument)<!> {}
    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) open <!CONFLICTING_OVERLOADS!>fun testDifferencesInValueParameterDefaultArgumentsPresenceReverse(arg: UserKlass)<!> {}

    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) open <!CONFLICTING_OVERLOADS!>fun testValueParameterWithDifferentDefaultArguments(arg: UserKlass = defaultArgumentA)<!> {}
    open <!CONFLICTING_OVERLOADS!>fun testValueParameterWithDifferentDefaultArguments(arg: UserKlass = defaultArgumentB)<!> {}

    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) open <!CONFLICTING_OVERLOADS!>fun testValueParameterWithAliasedDefaultArguments(arg: UserKlass = defaultArgument)<!> {}
    open <!CONFLICTING_OVERLOADS!>fun testValueParameterWithAliasedDefaultArguments(arg: UserKlass = sameDefaultArgument)<!> {}

    open <!CONFLICTING_OVERLOADS!>fun testValueParameterWithAliasedDefaultArgumentsReverse(arg: UserKlass = defaultArgument)<!> {}
    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) open <!CONFLICTING_OVERLOADS!>fun testValueParameterWithAliasedDefaultArgumentsReverse(arg: UserKlass = sameDefaultArgument)<!> {}

    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) open <!CONFLICTING_OVERLOADS!>fun <T> testIdenticalTypeParametersA()<!> {}
    open <!CONFLICTING_OVERLOADS!>fun <T> testIdenticalTypeParametersA()<!> {}

    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) open <!CONFLICTING_OVERLOADS!>fun <T> testIdenticalTypeParametersB(arg: T)<!> {}
    open <!CONFLICTING_OVERLOADS!>fun <T> testIdenticalTypeParametersB(arg: T)<!> {}

    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) open <!CONFLICTING_OVERLOADS!>fun <T> testIdenticalTypeParametersC(arg: Invariant<T>)<!> {}
    open <!CONFLICTING_OVERLOADS!>fun <T> testIdenticalTypeParametersC(arg: Invariant<T>)<!> {}

    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) open <!CONFLICTING_OVERLOADS!>fun <TA> testDifferentlyNamedTypeParametersA()<!> {}
    open <!CONFLICTING_OVERLOADS!>fun <TB> testDifferentlyNamedTypeParametersA()<!> {}

    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) open <!CONFLICTING_OVERLOADS!>fun <TA> testDifferentlyNamedTypeParametersB(arg: TA)<!> {}
    open <!CONFLICTING_OVERLOADS!>fun <TB> testDifferentlyNamedTypeParametersB(arg: TB)<!> {}

    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) open <!CONFLICTING_OVERLOADS!>fun <TA> testDifferentlyNamedTypeParametersC(arg: Invariant<TA>)<!> {}
    open <!CONFLICTING_OVERLOADS!>fun <TB> testDifferentlyNamedTypeParametersC(arg: Invariant<TB>)<!> {}

    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) open <!CONFLICTING_OVERLOADS!>fun <T1, T2> testMultipleIdenticalTypeParameters()<!> {}
    open <!CONFLICTING_OVERLOADS!>fun <T1, T2> testMultipleIdenticalTypeParameters()<!> {}

    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) open <!CONFLICTING_OVERLOADS!>fun <T1, T2A> testMultipleDifferentlyNamedTypeParametersA()<!> {}
    open <!CONFLICTING_OVERLOADS!>fun <T1, T2B> testMultipleDifferentlyNamedTypeParametersA()<!> {}

    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) open <!CONFLICTING_OVERLOADS!>fun <T1A, T2A> testMultipleDifferentlyNamedTypeParametersB()<!> {}
    open <!CONFLICTING_OVERLOADS!>fun <T1B, T2B> testMultipleDifferentlyNamedTypeParametersB()<!> {}

    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) open <!CONFLICTING_OVERLOADS!>fun <T: UserInterface> testTypeParameterWithIdenticalUpperBoundsA()<!> {}
    open <!CONFLICTING_OVERLOADS!>fun <T: UserInterface> testTypeParameterWithIdenticalUpperBoundsA()<!> {}

    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) open <!CONFLICTING_OVERLOADS!>fun <T: UserInterface> testTypeParameterWithIdenticalUpperBoundsB(arg: T)<!> {}
    open <!CONFLICTING_OVERLOADS!>fun <T: UserInterface> testTypeParameterWithIdenticalUpperBoundsB(arg: T)<!> {}

    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) open <!CONFLICTING_OVERLOADS!>fun <T: UserInterface> testTypeParameterWithIdenticalUpperBoundsC(arg: Invariant<T>)<!> {}
    open <!CONFLICTING_OVERLOADS!>fun <T: UserInterface> testTypeParameterWithIdenticalUpperBoundsC(arg: Invariant<T>)<!> {}

    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) open <!CONFLICTING_OVERLOADS!>fun <T: UserInterface> testDifferencesInTypeParameterUpperBoundsPresence()<!> {}
    open <!CONFLICTING_OVERLOADS!>fun <T> testDifferencesInTypeParameterUpperBoundsPresence()<!> {}

    open <!CONFLICTING_OVERLOADS!>fun <T: UserInterface> testDifferencesInTypeParameterUpperBoundsPresenceReverse()<!> {}
    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) open <!CONFLICTING_OVERLOADS!>fun <T> testDifferencesInTypeParameterUpperBoundsPresenceReverse()<!> {}

    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) open <!CONFLICTING_OVERLOADS!>fun <T: UserInterfaceA> testTypeParameterWithDifferentUpperBounds()<!> {}
    open <!CONFLICTING_OVERLOADS!>fun <T: UserInterfaceB> testTypeParameterWithDifferentUpperBounds()<!> {}

    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) open <!CONFLICTING_OVERLOADS!>fun <T: Invariant<UserInterface>> testTypeParameterWithVarianceDifferentUpperBoundsA()<!> {}
    open <!CONFLICTING_OVERLOADS!>fun <T: Invariant<out UserInterface>> testTypeParameterWithVarianceDifferentUpperBoundsA()<!> {}

    open <!CONFLICTING_OVERLOADS!>fun <T: Invariant<UserInterface>> testTypeParameterWithVarianceDifferentUpperBoundsAReverse()<!> {}
    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) open <!CONFLICTING_OVERLOADS!>fun <T: Invariant<out UserInterface>> testTypeParameterWithVarianceDifferentUpperBoundsAReverse()<!> {}

    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) open <!CONFLICTING_OVERLOADS!>fun <T: Invariant<UserInterface>> testTypeParameterWithVarianceDifferentUpperBoundsB()<!> {}
    open <!CONFLICTING_OVERLOADS!>fun <T: Invariant<in UserInterface>> testTypeParameterWithVarianceDifferentUpperBoundsB()<!> {}

    open <!CONFLICTING_OVERLOADS!>fun <T: Invariant<UserInterface>> testTypeParameterWithVarianceDifferentUpperBoundsBReverse()<!> {}
    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) open <!CONFLICTING_OVERLOADS!>fun <T: Invariant<in UserInterface>> testTypeParameterWithVarianceDifferentUpperBoundsBReverse()<!> {}

    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) open <!CONFLICTING_OVERLOADS!>fun <T: Invariant<UserInterface>> testTypeParameterWithVarianceDifferentUpperBoundsC()<!> {}
    open <!CONFLICTING_OVERLOADS!>fun <T: Invariant<*>> testTypeParameterWithVarianceDifferentUpperBoundsC()<!> {}

    open <!CONFLICTING_OVERLOADS!>fun <T: Invariant<UserInterface>> testTypeParameterWithVarianceDifferentUpperBoundsCReverse()<!> {}
    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) open <!CONFLICTING_OVERLOADS!>fun <T: Invariant<*>> testTypeParameterWithVarianceDifferentUpperBoundsCReverse()<!> {}

    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) open <!CONFLICTING_OVERLOADS!>fun <T: Invariant<out UserInterface>> testTypeParameterWithVarianceDifferentUpperBoundsD()<!> {}
    open <!CONFLICTING_OVERLOADS!>fun <T: Invariant<*>> testTypeParameterWithVarianceDifferentUpperBoundsD()<!> {}

    open <!CONFLICTING_OVERLOADS!>fun <T: Invariant<out UserInterface>> testTypeParameterWithVarianceDifferentUpperBoundsDReverse()<!> {}
    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) open <!CONFLICTING_OVERLOADS!>fun <T: Invariant<*>> testTypeParameterWithVarianceDifferentUpperBoundsDReverse()<!> {}

    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) open <!CONFLICTING_OVERLOADS!>fun <T: Invariant<in UserInterface>> testTypeParameterWithVarianceDifferentUpperBoundsE()<!> {}
    open <!CONFLICTING_OVERLOADS!>fun <T: Invariant<*>> testTypeParameterWithVarianceDifferentUpperBoundsE()<!> {}

    open <!CONFLICTING_OVERLOADS!>fun <T: Invariant<in UserInterface>> testTypeParameterWithVarianceDifferentUpperBoundsEReverse()<!> {}
    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) open <!CONFLICTING_OVERLOADS!>fun <T: Invariant<*>> testTypeParameterWithVarianceDifferentUpperBoundsEReverse()<!> {}

    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) open <!CONFLICTING_OVERLOADS!>fun <T: Invariant<out UserInterface>> testTypeParameterWithVarianceDifferentUpperBoundsF()<!> {}
    open <!CONFLICTING_OVERLOADS!>fun <T: Invariant<in UserInterface>> testTypeParameterWithVarianceDifferentUpperBoundsF()<!> {}

    open <!CONFLICTING_OVERLOADS!>fun <T: Invariant<out UserInterface>> testTypeParameterWithVarianceDifferentUpperBoundsFReverse()<!> {}
    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) open <!CONFLICTING_OVERLOADS!>fun <T: Invariant<in UserInterface>> testTypeParameterWithVarianceDifferentUpperBoundsFReverse()<!> {}

    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) open <!CONFLICTING_OVERLOADS!>fun <T: UserInterface> testTypeParameterWithTypeAliasedUpperBoundsA()<!> {}
    open <!CONFLICTING_OVERLOADS!>fun <T: SameUserInterface> testTypeParameterWithTypeAliasedUpperBoundsA()<!> {}

    open <!CONFLICTING_OVERLOADS!>fun <T: UserInterface> testTypeParameterWithTypeAliasedUpperBoundsAReverse()<!> {}
    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) open <!CONFLICTING_OVERLOADS!>fun <T: SameUserInterface> testTypeParameterWithTypeAliasedUpperBoundsAReverse()<!> {}

    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) open <!CONFLICTING_OVERLOADS!>fun <T: UserInterface> testTypeParameterWithTypeAliasedUpperBoundsB(arg: T)<!> {}
    open <!CONFLICTING_OVERLOADS!>fun <T: SameUserInterface> testTypeParameterWithTypeAliasedUpperBoundsB(arg: T)<!> {}

    open <!CONFLICTING_OVERLOADS!>fun <T: UserInterface> testTypeParameterWithTypeAliasedUpperBoundsBReverse(arg: T)<!> {}
    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) open <!CONFLICTING_OVERLOADS!>fun <T: SameUserInterface> testTypeParameterWithTypeAliasedUpperBoundsBReverse(arg: T)<!> {}

    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) open <!CONFLICTING_OVERLOADS!>fun <T: UserInterface> testTypeParameterWithTypeAliasedUpperBoundsC(arg: Invariant<T>)<!> {}
    open <!CONFLICTING_OVERLOADS!>fun <T: SameUserInterface> testTypeParameterWithTypeAliasedUpperBoundsC(arg: Invariant<T>)<!> {}

    open <!CONFLICTING_OVERLOADS!>fun <T: UserInterface> testTypeParameterWithTypeAliasedUpperBoundsCReverse(arg: Invariant<T>)<!> {}
    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) open <!CONFLICTING_OVERLOADS!>fun <T: SameUserInterface> testTypeParameterWithTypeAliasedUpperBoundsCReverse(arg: Invariant<T>)<!> {}

    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) open <!CONFLICTING_OVERLOADS!>fun <T> testTypeParameterWithMultipleIdenticalUpperBoundsAA()<!> where T: UserInterfaceA, T: UserInterfaceB {}
    open <!CONFLICTING_OVERLOADS!>fun <T> testTypeParameterWithMultipleIdenticalUpperBoundsAA()<!> where T: UserInterfaceA, T: UserInterfaceB {}

    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) open <!CONFLICTING_OVERLOADS!>fun <T> testTypeParameterWithMultipleIdenticalUpperBoundsAB(arg: T)<!> where T: UserInterfaceA, T: UserInterfaceB {}
    open <!CONFLICTING_OVERLOADS!>fun <T> testTypeParameterWithMultipleIdenticalUpperBoundsAB(arg: T)<!> where T: UserInterfaceA, T: UserInterfaceB {}

    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) open <!CONFLICTING_OVERLOADS!>fun <T> testTypeParameterWithMultipleIdenticalUpperBoundsAC(arg: Invariant<T>)<!> where T: UserInterfaceA, T: UserInterfaceB {}
    open <!CONFLICTING_OVERLOADS!>fun <T> testTypeParameterWithMultipleIdenticalUpperBoundsAC(arg: Invariant<T>)<!> where T: UserInterfaceA, T: UserInterfaceB {}

    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) open <!CONFLICTING_OVERLOADS!>fun <T: UserInterfaceA> testTypeParameterWithMultipleIdenticalUpperBoundsBA()<!> where T: UserInterfaceB {}
    open <!CONFLICTING_OVERLOADS!>fun <T: UserInterfaceA> testTypeParameterWithMultipleIdenticalUpperBoundsBA()<!> where T: UserInterfaceB {}

    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) open <!CONFLICTING_OVERLOADS!>fun <T: UserInterfaceA> testTypeParameterWithMultipleIdenticalUpperBoundsBB(arg: T)<!> where T: UserInterfaceB {}
    open <!CONFLICTING_OVERLOADS!>fun <T: UserInterfaceA> testTypeParameterWithMultipleIdenticalUpperBoundsBB(arg: T)<!> where T: UserInterfaceB {}

    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) open <!CONFLICTING_OVERLOADS!>fun <T: UserInterfaceA> testTypeParameterWithMultipleIdenticalUpperBoundsBC(arg: Invariant<T>)<!> where T: UserInterfaceB {}
    open <!CONFLICTING_OVERLOADS!>fun <T: UserInterfaceA> testTypeParameterWithMultipleIdenticalUpperBoundsBC(arg: Invariant<T>)<!> where T: UserInterfaceB {}

    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) open <!CONFLICTING_OVERLOADS!>fun <T> testDifferencesInTypeParameterMultipleUpperBoundsPresenceA()<!> where T: UserInterfaceA, T: UserInterfaceB {}
    open <!CONFLICTING_OVERLOADS!>fun <T> testDifferencesInTypeParameterMultipleUpperBoundsPresenceA()<!> where T: UserInterfaceA {}

    open <!CONFLICTING_OVERLOADS!>fun <T: UserInterfaceA> testDifferencesInTypeParameterMultipleUpperBoundsPresenceB()<!> where T: UserInterfaceB {}
    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) open <!CONFLICTING_OVERLOADS!>fun <T: UserInterfaceA> testDifferencesInTypeParameterMultipleUpperBoundsPresenceB()<!> {}

    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) open <!CONFLICTING_OVERLOADS!>fun <T> testTypeParameterWithMultipleDifferentUpperBoundsAA()<!> where T: UserInterfaceA, T: UserInterfaceB {}
    open <!CONFLICTING_OVERLOADS!>fun <T> testTypeParameterWithMultipleDifferentUpperBoundsAA()<!> where T: UserInterfaceA, T: UserInterfaceC {}

    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) open <!CONFLICTING_OVERLOADS!>fun <T: UserInterfaceA> testTypeParameterWithMultipleDifferentUpperBoundsAB()<!> where T: UserInterfaceB {}
    open <!CONFLICTING_OVERLOADS!>fun <T: UserInterfaceA> testTypeParameterWithMultipleDifferentUpperBoundsAB()<!> where T: UserInterfaceC {}

    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) open <!CONFLICTING_OVERLOADS!>fun <T: UserInterfaceB> testTypeParameterWithMultipleDifferentUpperBoundsAC()<!> where T: UserInterfaceA {}
    open <!CONFLICTING_OVERLOADS!>fun <T: UserInterfaceC> testTypeParameterWithMultipleDifferentUpperBoundsAC()<!> where T: UserInterfaceA {}

    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) open <!CONFLICTING_OVERLOADS!>fun <T> testTypeParameterWithMultipleDifferentUpperBoundsBA()<!> where T: UserInterfaceA, T: UserInterfaceB {}
    open <!CONFLICTING_OVERLOADS!>fun <T> testTypeParameterWithMultipleDifferentUpperBoundsBA()<!> where T: UserInterfaceC, T: UserInterfaceD {}

    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) open <!CONFLICTING_OVERLOADS!>fun <T: UserInterfaceA> testTypeParameterWithMultipleDifferentUpperBoundsBB()<!> where T: UserInterfaceB {}
    open <!CONFLICTING_OVERLOADS!>fun <T: UserInterfaceC> testTypeParameterWithMultipleDifferentUpperBoundsBB()<!> where T: UserInterfaceD {}

    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) open <!CONFLICTING_OVERLOADS!>fun <T> testTypeParameterWithMultipleTypeAliasedUpperBoundsAAA()<!> where T: UserInterfaceA, T: UserInterfaceB {}
    open <!CONFLICTING_OVERLOADS!>fun <T> testTypeParameterWithMultipleTypeAliasedUpperBoundsAAA()<!> where T: UserInterfaceA, T: SameUserInterfaceB {}

    open <!CONFLICTING_OVERLOADS!>fun <T> testTypeParameterWithMultipleTypeAliasedUpperBoundsAAAReverse()<!> where T: UserInterfaceA, T: UserInterfaceB {}
    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) open <!CONFLICTING_OVERLOADS!>fun <T> testTypeParameterWithMultipleTypeAliasedUpperBoundsAAAReverse()<!> where T: UserInterfaceA, T: SameUserInterfaceB {}

    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) open <!CONFLICTING_OVERLOADS!>fun <T> testTypeParameterWithMultipleTypeAliasedUpperBoundsAAB(arg: T)<!> where T: UserInterfaceA, T: UserInterfaceB {}
    open <!CONFLICTING_OVERLOADS!>fun <T> testTypeParameterWithMultipleTypeAliasedUpperBoundsAAB(arg: T)<!> where T: UserInterfaceA, T: SameUserInterfaceB {}

    open <!CONFLICTING_OVERLOADS!>fun <T> testTypeParameterWithMultipleTypeAliasedUpperBoundsAABReverse(arg: T)<!> where T: UserInterfaceA, T: UserInterfaceB {}
    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) open <!CONFLICTING_OVERLOADS!>fun <T> testTypeParameterWithMultipleTypeAliasedUpperBoundsAABReverse(arg: T)<!> where T: UserInterfaceA, T: SameUserInterfaceB {}

    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) open <!CONFLICTING_OVERLOADS!>fun <T> testTypeParameterWithMultipleTypeAliasedUpperBoundsAAC(arg: Invariant<T>)<!> where T: UserInterfaceA, T: UserInterfaceB {}
    open <!CONFLICTING_OVERLOADS!>fun <T> testTypeParameterWithMultipleTypeAliasedUpperBoundsAAC(arg: Invariant<T>)<!> where T: UserInterfaceA, T: SameUserInterfaceB {}

    open <!CONFLICTING_OVERLOADS!>fun <T> testTypeParameterWithMultipleTypeAliasedUpperBoundsAACReverse(arg: Invariant<T>)<!> where T: UserInterfaceA, T: UserInterfaceB {}
    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) open <!CONFLICTING_OVERLOADS!>fun <T> testTypeParameterWithMultipleTypeAliasedUpperBoundsAACReverse(arg: Invariant<T>)<!> where T: UserInterfaceA, T: SameUserInterfaceB {}

    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) open <!CONFLICTING_OVERLOADS!>fun <T: UserInterfaceA> testTypeParameterWithMultipleTypeAliasedUpperBoundsABA()<!> where T: UserInterfaceB {}
    open <!CONFLICTING_OVERLOADS!>fun <T: UserInterfaceA> testTypeParameterWithMultipleTypeAliasedUpperBoundsABA()<!> where T: SameUserInterfaceB {}

    open <!CONFLICTING_OVERLOADS!>fun <T: UserInterfaceA> testTypeParameterWithMultipleTypeAliasedUpperBoundsABAReverse()<!> where T: UserInterfaceB {}
    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) open <!CONFLICTING_OVERLOADS!>fun <T: UserInterfaceA> testTypeParameterWithMultipleTypeAliasedUpperBoundsABAReverse()<!> where T: SameUserInterfaceB {}

    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) open <!CONFLICTING_OVERLOADS!>fun <T: UserInterfaceA> testTypeParameterWithMultipleTypeAliasedUpperBoundsABB(arg: T)<!> where T: UserInterfaceB {}
    open <!CONFLICTING_OVERLOADS!>fun <T: UserInterfaceA> testTypeParameterWithMultipleTypeAliasedUpperBoundsABB(arg: T)<!> where T: SameUserInterfaceB {}

    open <!CONFLICTING_OVERLOADS!>fun <T: UserInterfaceA> testTypeParameterWithMultipleTypeAliasedUpperBoundsABBReverse(arg: T)<!> where T: UserInterfaceB {}
    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) open <!CONFLICTING_OVERLOADS!>fun <T: UserInterfaceA> testTypeParameterWithMultipleTypeAliasedUpperBoundsABBReverse(arg: T)<!> where T: SameUserInterfaceB {}

    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) open <!CONFLICTING_OVERLOADS!>fun <T: UserInterfaceA> testTypeParameterWithMultipleTypeAliasedUpperBoundsABC(arg: Invariant<T>)<!> where T: UserInterfaceB {}
    open <!CONFLICTING_OVERLOADS!>fun <T: UserInterfaceA> testTypeParameterWithMultipleTypeAliasedUpperBoundsABC(arg: Invariant<T>)<!> where T: SameUserInterfaceB {}

    open <!CONFLICTING_OVERLOADS!>fun <T: UserInterfaceA> testTypeParameterWithMultipleTypeAliasedUpperBoundsABCReverse(arg: Invariant<T>)<!> where T: UserInterfaceB {}
    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) open <!CONFLICTING_OVERLOADS!>fun <T: UserInterfaceA> testTypeParameterWithMultipleTypeAliasedUpperBoundsABCReverse(arg: Invariant<T>)<!> where T: SameUserInterfaceB {}

    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) open <!CONFLICTING_OVERLOADS!>fun <T: UserInterfaceB> testTypeParameterWithMultipleTypeAliasedUpperBoundsACA()<!> where T: UserInterfaceA {}
    open <!CONFLICTING_OVERLOADS!>fun <T: SameUserInterfaceB> testTypeParameterWithMultipleTypeAliasedUpperBoundsACA()<!> where T: UserInterfaceA {}

    open <!CONFLICTING_OVERLOADS!>fun <T: UserInterfaceB> testTypeParameterWithMultipleTypeAliasedUpperBoundsACAReverse()<!> where T: UserInterfaceA {}
    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) open <!CONFLICTING_OVERLOADS!>fun <T: SameUserInterfaceB> testTypeParameterWithMultipleTypeAliasedUpperBoundsACAReverse()<!> where T: UserInterfaceA {}

    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) open <!CONFLICTING_OVERLOADS!>fun <T: UserInterfaceB> testTypeParameterWithMultipleTypeAliasedUpperBoundsACB(arg: T)<!> where T: UserInterfaceA {}
    open <!CONFLICTING_OVERLOADS!>fun <T: SameUserInterfaceB> testTypeParameterWithMultipleTypeAliasedUpperBoundsACB(arg: T)<!> where T: UserInterfaceA {}

    open <!CONFLICTING_OVERLOADS!>fun <T: UserInterfaceB> testTypeParameterWithMultipleTypeAliasedUpperBoundsACBReverse(arg: T)<!> where T: UserInterfaceA {}
    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) open <!CONFLICTING_OVERLOADS!>fun <T: SameUserInterfaceB> testTypeParameterWithMultipleTypeAliasedUpperBoundsACBReverse(arg: T)<!> where T: UserInterfaceA {}

    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) open <!CONFLICTING_OVERLOADS!>fun <T: UserInterfaceB> testTypeParameterWithMultipleTypeAliasedUpperBoundsACC(arg: Invariant<T>)<!> where T: UserInterfaceA {}
    open <!CONFLICTING_OVERLOADS!>fun <T: SameUserInterfaceB> testTypeParameterWithMultipleTypeAliasedUpperBoundsACC(arg: Invariant<T>)<!> where T: UserInterfaceA {}

    open <!CONFLICTING_OVERLOADS!>fun <T: UserInterfaceB> testTypeParameterWithMultipleTypeAliasedUpperBoundsACCReverse(arg: Invariant<T>)<!> where T: UserInterfaceA {}
    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) open <!CONFLICTING_OVERLOADS!>fun <T: SameUserInterfaceB> testTypeParameterWithMultipleTypeAliasedUpperBoundsACCReverse(arg: Invariant<T>)<!> where T: UserInterfaceA {}

    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) open <!CONFLICTING_OVERLOADS!>fun <T> testTypeParameterWithMultipleTypeAliasedUpperBoundsBAA()<!> where T: UserInterfaceA, T: UserInterfaceB {}
    open <!CONFLICTING_OVERLOADS!>fun <T> testTypeParameterWithMultipleTypeAliasedUpperBoundsBAA()<!> where T: SameUserInterfaceA, T: SameUserInterfaceB {}

    open <!CONFLICTING_OVERLOADS!>fun <T> testTypeParameterWithMultipleTypeAliasedUpperBoundsBAAReverse()<!> where T: UserInterfaceA, T: UserInterfaceB {}
    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) open <!CONFLICTING_OVERLOADS!>fun <T> testTypeParameterWithMultipleTypeAliasedUpperBoundsBAAReverse()<!> where T: SameUserInterfaceA, T: SameUserInterfaceB {}

    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) open <!CONFLICTING_OVERLOADS!>fun <T> testTypeParameterWithMultipleTypeAliasedUpperBoundsBAB(arg: T)<!> where T: UserInterfaceA, T: UserInterfaceB {}
    open <!CONFLICTING_OVERLOADS!>fun <T> testTypeParameterWithMultipleTypeAliasedUpperBoundsBAB(arg: T)<!> where T: SameUserInterfaceA, T: SameUserInterfaceB {}

    open <!CONFLICTING_OVERLOADS!>fun <T> testTypeParameterWithMultipleTypeAliasedUpperBoundsBABReverse(arg: T)<!> where T: UserInterfaceA, T: UserInterfaceB {}
    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) open <!CONFLICTING_OVERLOADS!>fun <T> testTypeParameterWithMultipleTypeAliasedUpperBoundsBABReverse(arg: T)<!> where T: SameUserInterfaceA, T: SameUserInterfaceB {}

    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) open <!CONFLICTING_OVERLOADS!>fun <T> testTypeParameterWithMultipleTypeAliasedUpperBoundsBAC(arg: Invariant<T>)<!> where T: UserInterfaceA, T: UserInterfaceB {}
    open <!CONFLICTING_OVERLOADS!>fun <T> testTypeParameterWithMultipleTypeAliasedUpperBoundsBAC(arg: Invariant<T>)<!> where T: SameUserInterfaceA, T: SameUserInterfaceB {}

    open <!CONFLICTING_OVERLOADS!>fun <T> testTypeParameterWithMultipleTypeAliasedUpperBoundsBACReverse(arg: Invariant<T>)<!> where T: UserInterfaceA, T: UserInterfaceB {}
    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) open <!CONFLICTING_OVERLOADS!>fun <T> testTypeParameterWithMultipleTypeAliasedUpperBoundsBACReverse(arg: Invariant<T>)<!> where T: SameUserInterfaceA, T: SameUserInterfaceB {}

    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) open <!CONFLICTING_OVERLOADS!>fun <T: UserInterfaceA> testTypeParameterWithMultipleTypeAliasedUpperBoundsBBA()<!> where T: UserInterfaceB {}
    open <!CONFLICTING_OVERLOADS!>fun <T: SameUserInterfaceA> testTypeParameterWithMultipleTypeAliasedUpperBoundsBBA()<!> where T: SameUserInterfaceB {}

    open <!CONFLICTING_OVERLOADS!>fun <T: UserInterfaceA> testTypeParameterWithMultipleTypeAliasedUpperBoundsBBAReverse()<!> where T: UserInterfaceB {}
    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) open <!CONFLICTING_OVERLOADS!>fun <T: SameUserInterfaceA> testTypeParameterWithMultipleTypeAliasedUpperBoundsBBAReverse()<!> where T: SameUserInterfaceB {}

    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) open <!CONFLICTING_OVERLOADS!>fun <T: UserInterfaceA> testTypeParameterWithMultipleTypeAliasedUpperBoundsBBB(arg: T)<!> where T: UserInterfaceB {}
    open <!CONFLICTING_OVERLOADS!>fun <T: SameUserInterfaceA> testTypeParameterWithMultipleTypeAliasedUpperBoundsBBB(arg: T)<!> where T: SameUserInterfaceB {}

    open <!CONFLICTING_OVERLOADS!>fun <T: UserInterfaceA> testTypeParameterWithMultipleTypeAliasedUpperBoundsBBBReverse(arg: T)<!> where T: UserInterfaceB {}
    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) open <!CONFLICTING_OVERLOADS!>fun <T: SameUserInterfaceA> testTypeParameterWithMultipleTypeAliasedUpperBoundsBBBReverse(arg: T)<!> where T: SameUserInterfaceB {}

    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) open <!CONFLICTING_OVERLOADS!>fun <T: UserInterfaceA> testTypeParameterWithMultipleTypeAliasedUpperBoundsBBC(arg: Invariant<T>)<!> where T: UserInterfaceB {}
    open <!CONFLICTING_OVERLOADS!>fun <T: SameUserInterfaceA> testTypeParameterWithMultipleTypeAliasedUpperBoundsBBC(arg: Invariant<T>)<!> where T: SameUserInterfaceB {}

    open <!CONFLICTING_OVERLOADS!>fun <T: UserInterfaceA> testTypeParameterWithMultipleTypeAliasedUpperBoundsBBCReverse(arg: Invariant<T>)<!> where T: UserInterfaceB {}
    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) open <!CONFLICTING_OVERLOADS!>fun <T: SameUserInterfaceA> testTypeParameterWithMultipleTypeAliasedUpperBoundsBBCReverse(arg: Invariant<T>)<!> where T: SameUserInterfaceB {}

    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) open <!CONFLICTING_OVERLOADS!>fun <T> testTypeParameterWithMultipleShuffledUpperBoundsAA()<!> where T: UserInterfaceA, T: UserInterfaceB {}
    open <!CONFLICTING_OVERLOADS!>fun <T> testTypeParameterWithMultipleShuffledUpperBoundsAA()<!> where T: UserInterfaceB, T: UserInterfaceA {}

    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) open <!CONFLICTING_OVERLOADS!>fun <T> testTypeParameterWithMultipleShuffledUpperBoundsAB(arg: T)<!> where T: UserInterfaceA, T: UserInterfaceB {}
    open <!CONFLICTING_OVERLOADS!>fun <T> testTypeParameterWithMultipleShuffledUpperBoundsAB(arg: T)<!> where T: UserInterfaceB, T: UserInterfaceA {}

    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) open <!CONFLICTING_OVERLOADS!>fun <T> testTypeParameterWithMultipleShuffledUpperBoundsAC(arg: Invariant<T>)<!> where T: UserInterfaceA, T: UserInterfaceB {}
    open <!CONFLICTING_OVERLOADS!>fun <T> testTypeParameterWithMultipleShuffledUpperBoundsAC(arg: Invariant<T>)<!> where T: UserInterfaceB, T: UserInterfaceA {}

    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) open <!CONFLICTING_OVERLOADS!>fun <T: UserInterfaceA> testTypeParameterWithMultipleShuffledUpperBoundsBA()<!> where T: UserInterfaceB {}
    open <!CONFLICTING_OVERLOADS!>fun <T: UserInterfaceB> testTypeParameterWithMultipleShuffledUpperBoundsBA()<!> where T: UserInterfaceA {}

    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) open <!CONFLICTING_OVERLOADS!>fun <T: UserInterfaceA> testTypeParameterWithMultipleShuffledUpperBoundsBB(arg: T)<!> where T: UserInterfaceB {}
    open <!CONFLICTING_OVERLOADS!>fun <T: UserInterfaceB> testTypeParameterWithMultipleShuffledUpperBoundsBB(arg: T)<!> where T: UserInterfaceA {}

    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) open <!CONFLICTING_OVERLOADS!>fun <T: UserInterfaceA> testTypeParameterWithMultipleShuffledUpperBoundsBC(arg: Invariant<T>)<!> where T: UserInterfaceB {}
    open <!CONFLICTING_OVERLOADS!>fun <T: UserInterfaceB> testTypeParameterWithMultipleShuffledUpperBoundsBC(arg: Invariant<T>)<!> where T: UserInterfaceA {}

    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) infix open <!CONFLICTING_OVERLOADS!>fun UserKlass.testIdenticalPresenceOfInfixModifier(arg: UserKlass)<!> {}
    infix open <!CONFLICTING_OVERLOADS!>fun UserKlass.testIdenticalPresenceOfInfixModifier(arg: UserKlass)<!> {}

    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) infix open <!CONFLICTING_OVERLOADS!>fun UserKlass.testDifferencesInInfixModifierPresence(arg: UserKlass)<!> {}
    open <!CONFLICTING_OVERLOADS!>fun UserKlass.testDifferencesInInfixModifierPresence(arg: UserKlass)<!> {}

    infix open <!CONFLICTING_OVERLOADS!>fun UserKlass.testDifferencesInInfixModifierPresenceReverse(arg: UserKlass)<!> {}
    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) open <!CONFLICTING_OVERLOADS!>fun UserKlass.testDifferencesInInfixModifierPresenceReverse(arg: UserKlass)<!> {}

    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) operator open <!CONFLICTING_OVERLOADS!>fun UserKlassA.unaryPlus()<!> {}
    operator open <!CONFLICTING_OVERLOADS!>fun UserKlassA.unaryPlus()<!> {}

    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) operator open <!CONFLICTING_OVERLOADS!>fun UserKlassB.unaryPlus()<!> {}
    open <!CONFLICTING_OVERLOADS!>fun UserKlassB.unaryPlus()<!> {}

    operator open <!CONFLICTING_OVERLOADS!>fun UserKlassB.unaryMinus()<!> {}
    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) open <!CONFLICTING_OVERLOADS!>fun UserKlassB.unaryMinus()<!> {}

    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) internal open <!CONFLICTING_OVERLOADS!>fun testIdenticalInternalVisibility()<!> {}
    internal open <!CONFLICTING_OVERLOADS!>fun testIdenticalInternalVisibility()<!> {}

    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) internal open <!CONFLICTING_OVERLOADS!>fun testDifferencesInInternalAndPublicVisibilities()<!> {}
    public open <!CONFLICTING_OVERLOADS!>fun testDifferencesInInternalAndPublicVisibilities()<!> {}

    internal open <!CONFLICTING_OVERLOADS!>fun testDifferencesInInternalAndPublicVisibilitiesReverse()<!> {}
    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) public open <!CONFLICTING_OVERLOADS!>fun testDifferencesInInternalAndPublicVisibilitiesReverse()<!> {}

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
