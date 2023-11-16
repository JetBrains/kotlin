// DIAGNOSTICS: -MISPLACED_TYPE_PARAMETER_CONSTRAINTS
// FIR_IDENTICAL

open class MemberScope {

    <!CONFLICTING_OVERLOADS!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) open fun testBasic()<!> {}
    <!CONFLICTING_OVERLOADS!>open fun testBasic()<!> {}
    
    <!CONFLICTING_OVERLOADS!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) open fun testIdenticalReturnTypes(): UserKlass<!> = UserKlass()
    <!CONFLICTING_OVERLOADS!>open fun testIdenticalReturnTypes(): UserKlass<!> = UserKlass()

    <!CONFLICTING_OVERLOADS!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) open fun testDifferencesInReturnTypePresence(): Unit<!> {}
    <!CONFLICTING_OVERLOADS!>open fun testDifferencesInReturnTypePresence()<!> {}

    <!CONFLICTING_OVERLOADS!>open fun testDifferencesInReturnTypePresenceReverse(): Unit<!> {}
    <!CONFLICTING_OVERLOADS!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) open fun testDifferencesInReturnTypePresenceReverse()<!> {}

    <!CONFLICTING_OVERLOADS!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) open fun testDifferentReturnTypes(): UserKlassA<!> = UserKlassA()
    <!CONFLICTING_OVERLOADS!>open fun testDifferentReturnTypes(): UserKlassB<!> = UserKlassB()

    <!CONFLICTING_OVERLOADS!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) open fun testVarianceDifferentReturnTypesA(): Invariant<UserKlass><!> = Invariant()
    <!CONFLICTING_OVERLOADS!>open fun testVarianceDifferentReturnTypesA(): Invariant<out UserKlass><!> = Invariant()

    <!CONFLICTING_OVERLOADS!>open fun testVarianceDifferentReturnTypesAReverse(): Invariant<UserKlass><!> = Invariant()
    <!CONFLICTING_OVERLOADS!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) open fun testVarianceDifferentReturnTypesAReverse(): Invariant<out UserKlass><!> = Invariant()

    <!CONFLICTING_OVERLOADS!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) open fun testVarianceDifferentReturnTypesB(): Invariant<UserKlass><!> = Invariant()
    <!CONFLICTING_OVERLOADS!>open fun testVarianceDifferentReturnTypesB(): Invariant<in UserKlass><!> = Invariant()

    <!CONFLICTING_OVERLOADS!>open fun testVarianceDifferentReturnTypesBReverse(): Invariant<UserKlass><!> = Invariant()
    <!CONFLICTING_OVERLOADS!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) open fun testVarianceDifferentReturnTypesBReverse(): Invariant<in UserKlass><!> = Invariant()

    <!CONFLICTING_OVERLOADS!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) open fun testVarianceDifferentReturnTypesC(): Invariant<UserKlass><!> = Invariant()
    <!CONFLICTING_OVERLOADS!>open fun testVarianceDifferentReturnTypesC(): Invariant<*><!> = Invariant<UserKlass>()

    <!CONFLICTING_OVERLOADS!>open fun testVarianceDifferentReturnTypesCReverse(): Invariant<UserKlass><!> = Invariant()
    <!CONFLICTING_OVERLOADS!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) open fun testVarianceDifferentReturnTypesCReverse(): Invariant<*><!> = Invariant<UserKlass>()

    <!CONFLICTING_OVERLOADS!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) open fun testVarianceDifferentReturnTypesD(): Invariant<out UserKlass><!> = Invariant()
    <!CONFLICTING_OVERLOADS!>open fun testVarianceDifferentReturnTypesD(): Invariant<*><!> = Invariant<UserKlass>()

    <!CONFLICTING_OVERLOADS!>open fun testVarianceDifferentReturnTypesDReverse(): Invariant<out UserKlass><!> = Invariant()
    <!CONFLICTING_OVERLOADS!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) open fun testVarianceDifferentReturnTypesDReverse(): Invariant<*><!> = Invariant<UserKlass>()

    <!CONFLICTING_OVERLOADS!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) open fun testVarianceDifferentReturnTypesE(): Invariant<in UserKlass><!> = Invariant()
    <!CONFLICTING_OVERLOADS!>open fun testVarianceDifferentReturnTypesE(): Invariant<*><!> = Invariant<UserKlass>()

    <!CONFLICTING_OVERLOADS!>open fun testVarianceDifferentReturnTypesEReverse(): Invariant<in UserKlass><!> = Invariant()
    <!CONFLICTING_OVERLOADS!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) open fun testVarianceDifferentReturnTypesEReverse(): Invariant<*><!> = Invariant<UserKlass>()

    <!CONFLICTING_OVERLOADS!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) open fun testVarianceDifferentReturnTypesF(): Invariant<out UserKlass><!> = Invariant()
    <!CONFLICTING_OVERLOADS!>open fun testVarianceDifferentReturnTypesF(): Invariant<in UserKlass><!> = Invariant()

    <!CONFLICTING_OVERLOADS!>open fun testVarianceDifferentReturnTypesFReverse(): Invariant<out UserKlass><!> = Invariant()
    <!CONFLICTING_OVERLOADS!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) open fun testVarianceDifferentReturnTypesFReverse(): Invariant<in UserKlass><!> = Invariant()

    <!CONFLICTING_OVERLOADS!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) open fun testTypeAliasedReturnTypes(): UserKlass<!> = UserKlass()
    <!CONFLICTING_OVERLOADS!>open fun testTypeAliasedReturnTypes(): SameUserKlass<!> = UserKlass()

    <!CONFLICTING_OVERLOADS!>open fun testTypeAliasedReturnTypesReverse(): UserKlass<!> = UserKlass()
    <!CONFLICTING_OVERLOADS!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) open fun testTypeAliasedReturnTypesReverse(): SameUserKlass<!> = UserKlass()

    <!CONFLICTING_OVERLOADS!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) open fun UserKlass.testIdenticalExtensionReceivers()<!> {}
    <!CONFLICTING_OVERLOADS!>open fun UserKlass.testIdenticalExtensionReceivers()<!> {}

    <!CONFLICTING_OVERLOADS!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) open fun UserKlass.testTypeAliasedExtensionReceivers()<!> {}
    <!CONFLICTING_OVERLOADS!>open fun SameUserKlass.testTypeAliasedExtensionReceivers()<!> {}

    <!CONFLICTING_OVERLOADS!>open fun UserKlass.testTypeAliasedExtensionReceiversReverse()<!> {}
    <!CONFLICTING_OVERLOADS!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) open fun SameUserKlass.testTypeAliasedExtensionReceiversReverse()<!> {}

    <!CONFLICTING_OVERLOADS!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) open fun testIdenticalValueParameters(arg: UserKlass)<!> {}
    <!CONFLICTING_OVERLOADS!>open fun testIdenticalValueParameters(arg: UserKlass)<!> {}

    <!CONFLICTING_OVERLOADS!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) open fun testDifferentlyNamedValueParameters(argA: UserKlass)<!> {}
    <!CONFLICTING_OVERLOADS!>open fun testDifferentlyNamedValueParameters(argB: UserKlass)<!> {}

    <!CONFLICTING_OVERLOADS!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) open fun testTypeAliasedValueParameterTypes(arg: UserKlass)<!> {}
    <!CONFLICTING_OVERLOADS!>open fun testTypeAliasedValueParameterTypes(arg: SameUserKlass)<!> {}

    <!CONFLICTING_OVERLOADS!>open fun testTypeAliasedValueParameterTypesReverse(arg: UserKlass)<!> {}
    <!CONFLICTING_OVERLOADS!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) open fun testTypeAliasedValueParameterTypesReverse(arg: SameUserKlass)<!> {}

    <!CONFLICTING_OVERLOADS!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) open fun testMultipleIdenticalValueParameters(arg1: UserKlassA, arg2: UserKlassB)<!> {}
    <!CONFLICTING_OVERLOADS!>open fun testMultipleIdenticalValueParameters(arg1: UserKlassA, arg2: UserKlassB)<!> {}

    <!CONFLICTING_OVERLOADS!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) open fun testMultipleDifferentlyNamedValueParametersA(arg1: UserKlassA, arg2A: UserKlassB)<!> {}
    <!CONFLICTING_OVERLOADS!>open fun testMultipleDifferentlyNamedValueParametersA(arg1: UserKlassA, arg2B: UserKlassB)<!> {}

    <!CONFLICTING_OVERLOADS!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) open fun testMultipleDifferentlyNamedValueParametersB(arg1A: UserKlassA, arg2A: UserKlassB)<!> {}
    <!CONFLICTING_OVERLOADS!>open fun testMultipleDifferentlyNamedValueParametersB(arg1B: UserKlassA, arg2B: UserKlassB)<!> {}

    <!CONFLICTING_OVERLOADS!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) open fun testMultipleTypeAliasedValueParameterTypesA(arg1: UserKlassA, arg2: UserKlassB)<!> {}
    <!CONFLICTING_OVERLOADS!>open fun testMultipleTypeAliasedValueParameterTypesA(arg1: UserKlassA, arg2: SameUserKlassB)<!> {}

    <!CONFLICTING_OVERLOADS!>open fun testMultipleTypeAliasedValueParameterTypesAReverse(arg1: UserKlassA, arg2: UserKlassB)<!> {}
    <!CONFLICTING_OVERLOADS!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) open fun testMultipleTypeAliasedValueParameterTypesAReverse(arg1: UserKlassA, arg2: SameUserKlassB)<!> {}

    <!CONFLICTING_OVERLOADS!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) open fun testMultipleTypeAliasedValueParameterTypesB(arg1: UserKlassA, arg2: UserKlassB)<!> {}
    <!CONFLICTING_OVERLOADS!>open fun testMultipleTypeAliasedValueParameterTypesB(arg1: SameUserKlassA, arg2: SameUserKlassB)<!> {}

    <!CONFLICTING_OVERLOADS!>open fun testMultipleTypeAliasedValueParameterTypesBReverse(arg1: UserKlassA, arg2: UserKlassB)<!> {}
    <!CONFLICTING_OVERLOADS!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) open fun testMultipleTypeAliasedValueParameterTypesBReverse(arg1: SameUserKlassA, arg2: SameUserKlassB)<!> {}

    <!CONFLICTING_OVERLOADS!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) open fun testValueParameterWithIdenticalDefaultArguments(arg: UserKlass = defaultArgument)<!> {}
    <!CONFLICTING_OVERLOADS!>open fun testValueParameterWithIdenticalDefaultArguments(arg: UserKlass = defaultArgument)<!> {}

    <!CONFLICTING_OVERLOADS!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) open fun testDifferencesInValueParameterDefaultArgumentsPresence(arg: UserKlass = defaultArgument)<!> {}
    <!CONFLICTING_OVERLOADS!>open fun testDifferencesInValueParameterDefaultArgumentsPresence(arg: UserKlass)<!> {}

    <!CONFLICTING_OVERLOADS!>open fun testDifferencesInValueParameterDefaultArgumentsPresenceReverse(arg: UserKlass = defaultArgument)<!> {}
    <!CONFLICTING_OVERLOADS!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) open fun testDifferencesInValueParameterDefaultArgumentsPresenceReverse(arg: UserKlass)<!> {}

    <!CONFLICTING_OVERLOADS!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) open fun testValueParameterWithDifferentDefaultArguments(arg: UserKlass = defaultArgumentA)<!> {}
    <!CONFLICTING_OVERLOADS!>open fun testValueParameterWithDifferentDefaultArguments(arg: UserKlass = defaultArgumentB)<!> {}

    <!CONFLICTING_OVERLOADS!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) open fun testValueParameterWithAliasedDefaultArguments(arg: UserKlass = defaultArgument)<!> {}
    <!CONFLICTING_OVERLOADS!>open fun testValueParameterWithAliasedDefaultArguments(arg: UserKlass = sameDefaultArgument)<!> {}

    <!CONFLICTING_OVERLOADS!>open fun testValueParameterWithAliasedDefaultArgumentsReverse(arg: UserKlass = defaultArgument)<!> {}
    <!CONFLICTING_OVERLOADS!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) open fun testValueParameterWithAliasedDefaultArgumentsReverse(arg: UserKlass = sameDefaultArgument)<!> {}

    <!CONFLICTING_OVERLOADS!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) open fun <T> testIdenticalTypeParametersA()<!> {}
    <!CONFLICTING_OVERLOADS!>open fun <T> testIdenticalTypeParametersA()<!> {}

    <!CONFLICTING_OVERLOADS!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) open fun <T> testIdenticalTypeParametersB(arg: T)<!> {}
    <!CONFLICTING_OVERLOADS!>open fun <T> testIdenticalTypeParametersB(arg: T)<!> {}

    <!CONFLICTING_OVERLOADS!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) open fun <T> testIdenticalTypeParametersC(arg: Invariant<T>)<!> {}
    <!CONFLICTING_OVERLOADS!>open fun <T> testIdenticalTypeParametersC(arg: Invariant<T>)<!> {}

    <!CONFLICTING_OVERLOADS!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) open fun <TA> testDifferentlyNamedTypeParametersA()<!> {}
    <!CONFLICTING_OVERLOADS!>open fun <TB> testDifferentlyNamedTypeParametersA()<!> {}

    <!CONFLICTING_OVERLOADS!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) open fun <TA> testDifferentlyNamedTypeParametersB(arg: TA)<!> {}
    <!CONFLICTING_OVERLOADS!>open fun <TB> testDifferentlyNamedTypeParametersB(arg: TB)<!> {}

    <!CONFLICTING_OVERLOADS!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) open fun <TA> testDifferentlyNamedTypeParametersC(arg: Invariant<TA>)<!> {}
    <!CONFLICTING_OVERLOADS!>open fun <TB> testDifferentlyNamedTypeParametersC(arg: Invariant<TB>)<!> {}

    <!CONFLICTING_OVERLOADS!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) open fun <T1, T2> testMultipleIdenticalTypeParameters()<!> {}
    <!CONFLICTING_OVERLOADS!>open fun <T1, T2> testMultipleIdenticalTypeParameters()<!> {}

    <!CONFLICTING_OVERLOADS!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) open fun <T1, T2A> testMultipleDifferentlyNamedTypeParametersA()<!> {}
    <!CONFLICTING_OVERLOADS!>open fun <T1, T2B> testMultipleDifferentlyNamedTypeParametersA()<!> {}

    <!CONFLICTING_OVERLOADS!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) open fun <T1A, T2A> testMultipleDifferentlyNamedTypeParametersB()<!> {}
    <!CONFLICTING_OVERLOADS!>open fun <T1B, T2B> testMultipleDifferentlyNamedTypeParametersB()<!> {}

    <!CONFLICTING_OVERLOADS!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) open fun <T: UserInterface> testTypeParameterWithIdenticalUpperBoundsA()<!> {}
    <!CONFLICTING_OVERLOADS!>open fun <T: UserInterface> testTypeParameterWithIdenticalUpperBoundsA()<!> {}

    <!CONFLICTING_OVERLOADS!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) open fun <T: UserInterface> testTypeParameterWithIdenticalUpperBoundsB(arg: T)<!> {}
    <!CONFLICTING_OVERLOADS!>open fun <T: UserInterface> testTypeParameterWithIdenticalUpperBoundsB(arg: T)<!> {}

    <!CONFLICTING_OVERLOADS!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) open fun <T: UserInterface> testTypeParameterWithIdenticalUpperBoundsC(arg: Invariant<T>)<!> {}
    <!CONFLICTING_OVERLOADS!>open fun <T: UserInterface> testTypeParameterWithIdenticalUpperBoundsC(arg: Invariant<T>)<!> {}

    <!CONFLICTING_OVERLOADS!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) open fun <T: UserInterface> testDifferencesInTypeParameterUpperBoundsPresence()<!> {}
    <!CONFLICTING_OVERLOADS!>open fun <T> testDifferencesInTypeParameterUpperBoundsPresence()<!> {}

    <!CONFLICTING_OVERLOADS!>open fun <T: UserInterface> testDifferencesInTypeParameterUpperBoundsPresenceReverse()<!> {}
    <!CONFLICTING_OVERLOADS!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) open fun <T> testDifferencesInTypeParameterUpperBoundsPresenceReverse()<!> {}

    <!CONFLICTING_OVERLOADS!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) open fun <T: UserInterfaceA> testTypeParameterWithDifferentUpperBounds()<!> {}
    <!CONFLICTING_OVERLOADS!>open fun <T: UserInterfaceB> testTypeParameterWithDifferentUpperBounds()<!> {}

    <!CONFLICTING_OVERLOADS!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) open fun <T: Invariant<UserInterface>> testTypeParameterWithVarianceDifferentUpperBoundsA()<!> {}
    <!CONFLICTING_OVERLOADS!>open fun <T: Invariant<out UserInterface>> testTypeParameterWithVarianceDifferentUpperBoundsA()<!> {}

    <!CONFLICTING_OVERLOADS!>open fun <T: Invariant<UserInterface>> testTypeParameterWithVarianceDifferentUpperBoundsAReverse()<!> {}
    <!CONFLICTING_OVERLOADS!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) open fun <T: Invariant<out UserInterface>> testTypeParameterWithVarianceDifferentUpperBoundsAReverse()<!> {}

    <!CONFLICTING_OVERLOADS!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) open fun <T: Invariant<UserInterface>> testTypeParameterWithVarianceDifferentUpperBoundsB()<!> {}
    <!CONFLICTING_OVERLOADS!>open fun <T: Invariant<in UserInterface>> testTypeParameterWithVarianceDifferentUpperBoundsB()<!> {}

    <!CONFLICTING_OVERLOADS!>open fun <T: Invariant<UserInterface>> testTypeParameterWithVarianceDifferentUpperBoundsBReverse()<!> {}
    <!CONFLICTING_OVERLOADS!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) open fun <T: Invariant<in UserInterface>> testTypeParameterWithVarianceDifferentUpperBoundsBReverse()<!> {}

    <!CONFLICTING_OVERLOADS!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) open fun <T: Invariant<UserInterface>> testTypeParameterWithVarianceDifferentUpperBoundsC()<!> {}
    <!CONFLICTING_OVERLOADS!>open fun <T: Invariant<*>> testTypeParameterWithVarianceDifferentUpperBoundsC()<!> {}

    <!CONFLICTING_OVERLOADS!>open fun <T: Invariant<UserInterface>> testTypeParameterWithVarianceDifferentUpperBoundsCReverse()<!> {}
    <!CONFLICTING_OVERLOADS!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) open fun <T: Invariant<*>> testTypeParameterWithVarianceDifferentUpperBoundsCReverse()<!> {}

    <!CONFLICTING_OVERLOADS!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) open fun <T: Invariant<out UserInterface>> testTypeParameterWithVarianceDifferentUpperBoundsD()<!> {}
    <!CONFLICTING_OVERLOADS!>open fun <T: Invariant<*>> testTypeParameterWithVarianceDifferentUpperBoundsD()<!> {}

    <!CONFLICTING_OVERLOADS!>open fun <T: Invariant<out UserInterface>> testTypeParameterWithVarianceDifferentUpperBoundsDReverse()<!> {}
    <!CONFLICTING_OVERLOADS!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) open fun <T: Invariant<*>> testTypeParameterWithVarianceDifferentUpperBoundsDReverse()<!> {}

    <!CONFLICTING_OVERLOADS!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) open fun <T: Invariant<in UserInterface>> testTypeParameterWithVarianceDifferentUpperBoundsE()<!> {}
    <!CONFLICTING_OVERLOADS!>open fun <T: Invariant<*>> testTypeParameterWithVarianceDifferentUpperBoundsE()<!> {}

    <!CONFLICTING_OVERLOADS!>open fun <T: Invariant<in UserInterface>> testTypeParameterWithVarianceDifferentUpperBoundsEReverse()<!> {}
    <!CONFLICTING_OVERLOADS!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) open fun <T: Invariant<*>> testTypeParameterWithVarianceDifferentUpperBoundsEReverse()<!> {}

    <!CONFLICTING_OVERLOADS!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) open fun <T: Invariant<out UserInterface>> testTypeParameterWithVarianceDifferentUpperBoundsF()<!> {}
    <!CONFLICTING_OVERLOADS!>open fun <T: Invariant<in UserInterface>> testTypeParameterWithVarianceDifferentUpperBoundsF()<!> {}

    <!CONFLICTING_OVERLOADS!>open fun <T: Invariant<out UserInterface>> testTypeParameterWithVarianceDifferentUpperBoundsFReverse()<!> {}
    <!CONFLICTING_OVERLOADS!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) open fun <T: Invariant<in UserInterface>> testTypeParameterWithVarianceDifferentUpperBoundsFReverse()<!> {}

    <!CONFLICTING_OVERLOADS!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) open fun <T: UserInterface> testTypeParameterWithTypeAliasedUpperBoundsA()<!> {}
    <!CONFLICTING_OVERLOADS!>open fun <T: SameUserInterface> testTypeParameterWithTypeAliasedUpperBoundsA()<!> {}

    <!CONFLICTING_OVERLOADS!>open fun <T: UserInterface> testTypeParameterWithTypeAliasedUpperBoundsAReverse()<!> {}
    <!CONFLICTING_OVERLOADS!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) open fun <T: SameUserInterface> testTypeParameterWithTypeAliasedUpperBoundsAReverse()<!> {}

    <!CONFLICTING_OVERLOADS!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) open fun <T: UserInterface> testTypeParameterWithTypeAliasedUpperBoundsB(arg: T)<!> {}
    <!CONFLICTING_OVERLOADS!>open fun <T: SameUserInterface> testTypeParameterWithTypeAliasedUpperBoundsB(arg: T)<!> {}

    <!CONFLICTING_OVERLOADS!>open fun <T: UserInterface> testTypeParameterWithTypeAliasedUpperBoundsBReverse(arg: T)<!> {}
    <!CONFLICTING_OVERLOADS!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) open fun <T: SameUserInterface> testTypeParameterWithTypeAliasedUpperBoundsBReverse(arg: T)<!> {}

    <!CONFLICTING_OVERLOADS!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) open fun <T: UserInterface> testTypeParameterWithTypeAliasedUpperBoundsC(arg: Invariant<T>)<!> {}
    <!CONFLICTING_OVERLOADS!>open fun <T: SameUserInterface> testTypeParameterWithTypeAliasedUpperBoundsC(arg: Invariant<T>)<!> {}

    <!CONFLICTING_OVERLOADS!>open fun <T: UserInterface> testTypeParameterWithTypeAliasedUpperBoundsCReverse(arg: Invariant<T>)<!> {}
    <!CONFLICTING_OVERLOADS!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) open fun <T: SameUserInterface> testTypeParameterWithTypeAliasedUpperBoundsCReverse(arg: Invariant<T>)<!> {}

    <!CONFLICTING_OVERLOADS!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) open fun <T> testTypeParameterWithMultipleIdenticalUpperBoundsAA()<!> where T: UserInterfaceA, T: UserInterfaceB {}
    <!CONFLICTING_OVERLOADS!>open fun <T> testTypeParameterWithMultipleIdenticalUpperBoundsAA()<!> where T: UserInterfaceA, T: UserInterfaceB {}

    <!CONFLICTING_OVERLOADS!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) open fun <T> testTypeParameterWithMultipleIdenticalUpperBoundsAB(arg: T)<!> where T: UserInterfaceA, T: UserInterfaceB {}
    <!CONFLICTING_OVERLOADS!>open fun <T> testTypeParameterWithMultipleIdenticalUpperBoundsAB(arg: T)<!> where T: UserInterfaceA, T: UserInterfaceB {}

    <!CONFLICTING_OVERLOADS!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) open fun <T> testTypeParameterWithMultipleIdenticalUpperBoundsAC(arg: Invariant<T>)<!> where T: UserInterfaceA, T: UserInterfaceB {}
    <!CONFLICTING_OVERLOADS!>open fun <T> testTypeParameterWithMultipleIdenticalUpperBoundsAC(arg: Invariant<T>)<!> where T: UserInterfaceA, T: UserInterfaceB {}

    <!CONFLICTING_OVERLOADS!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) open fun <T: UserInterfaceA> testTypeParameterWithMultipleIdenticalUpperBoundsBA()<!> where T: UserInterfaceB {}
    <!CONFLICTING_OVERLOADS!>open fun <T: UserInterfaceA> testTypeParameterWithMultipleIdenticalUpperBoundsBA()<!> where T: UserInterfaceB {}

    <!CONFLICTING_OVERLOADS!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) open fun <T: UserInterfaceA> testTypeParameterWithMultipleIdenticalUpperBoundsBB(arg: T)<!> where T: UserInterfaceB {}
    <!CONFLICTING_OVERLOADS!>open fun <T: UserInterfaceA> testTypeParameterWithMultipleIdenticalUpperBoundsBB(arg: T)<!> where T: UserInterfaceB {}

    <!CONFLICTING_OVERLOADS!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) open fun <T: UserInterfaceA> testTypeParameterWithMultipleIdenticalUpperBoundsBC(arg: Invariant<T>)<!> where T: UserInterfaceB {}
    <!CONFLICTING_OVERLOADS!>open fun <T: UserInterfaceA> testTypeParameterWithMultipleIdenticalUpperBoundsBC(arg: Invariant<T>)<!> where T: UserInterfaceB {}

    <!CONFLICTING_OVERLOADS!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) open fun <T> testDifferencesInTypeParameterMultipleUpperBoundsPresenceA()<!> where T: UserInterfaceA, T: UserInterfaceB {}
    <!CONFLICTING_OVERLOADS!>open fun <T> testDifferencesInTypeParameterMultipleUpperBoundsPresenceA()<!> where T: UserInterfaceA {}

    <!CONFLICTING_OVERLOADS!>open fun <T: UserInterfaceA> testDifferencesInTypeParameterMultipleUpperBoundsPresenceB()<!> where T: UserInterfaceB {}
    <!CONFLICTING_OVERLOADS!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) open fun <T: UserInterfaceA> testDifferencesInTypeParameterMultipleUpperBoundsPresenceB()<!> {}

    <!CONFLICTING_OVERLOADS!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) open fun <T> testTypeParameterWithMultipleDifferentUpperBoundsAA()<!> where T: UserInterfaceA, T: UserInterfaceB {}
    <!CONFLICTING_OVERLOADS!>open fun <T> testTypeParameterWithMultipleDifferentUpperBoundsAA()<!> where T: UserInterfaceA, T: UserInterfaceC {}

    <!CONFLICTING_OVERLOADS!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) open fun <T: UserInterfaceA> testTypeParameterWithMultipleDifferentUpperBoundsAB()<!> where T: UserInterfaceB {}
    <!CONFLICTING_OVERLOADS!>open fun <T: UserInterfaceA> testTypeParameterWithMultipleDifferentUpperBoundsAB()<!> where T: UserInterfaceC {}

    <!CONFLICTING_OVERLOADS!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) open fun <T: UserInterfaceB> testTypeParameterWithMultipleDifferentUpperBoundsAC()<!> where T: UserInterfaceA {}
    <!CONFLICTING_OVERLOADS!>open fun <T: UserInterfaceC> testTypeParameterWithMultipleDifferentUpperBoundsAC()<!> where T: UserInterfaceA {}

    <!CONFLICTING_OVERLOADS!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) open fun <T> testTypeParameterWithMultipleDifferentUpperBoundsBA()<!> where T: UserInterfaceA, T: UserInterfaceB {}
    <!CONFLICTING_OVERLOADS!>open fun <T> testTypeParameterWithMultipleDifferentUpperBoundsBA()<!> where T: UserInterfaceC, T: UserInterfaceD {}

    <!CONFLICTING_OVERLOADS!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) open fun <T: UserInterfaceA> testTypeParameterWithMultipleDifferentUpperBoundsBB()<!> where T: UserInterfaceB {}
    <!CONFLICTING_OVERLOADS!>open fun <T: UserInterfaceC> testTypeParameterWithMultipleDifferentUpperBoundsBB()<!> where T: UserInterfaceD {}

    <!CONFLICTING_OVERLOADS!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) open fun <T> testTypeParameterWithMultipleTypeAliasedUpperBoundsAAA()<!> where T: UserInterfaceA, T: UserInterfaceB {}
    <!CONFLICTING_OVERLOADS!>open fun <T> testTypeParameterWithMultipleTypeAliasedUpperBoundsAAA()<!> where T: UserInterfaceA, T: SameUserInterfaceB {}

    <!CONFLICTING_OVERLOADS!>open fun <T> testTypeParameterWithMultipleTypeAliasedUpperBoundsAAAReverse()<!> where T: UserInterfaceA, T: UserInterfaceB {}
    <!CONFLICTING_OVERLOADS!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) open fun <T> testTypeParameterWithMultipleTypeAliasedUpperBoundsAAAReverse()<!> where T: UserInterfaceA, T: SameUserInterfaceB {}

    <!CONFLICTING_OVERLOADS!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) open fun <T> testTypeParameterWithMultipleTypeAliasedUpperBoundsAAB(arg: T)<!> where T: UserInterfaceA, T: UserInterfaceB {}
    <!CONFLICTING_OVERLOADS!>open fun <T> testTypeParameterWithMultipleTypeAliasedUpperBoundsAAB(arg: T)<!> where T: UserInterfaceA, T: SameUserInterfaceB {}

    <!CONFLICTING_OVERLOADS!>open fun <T> testTypeParameterWithMultipleTypeAliasedUpperBoundsAABReverse(arg: T)<!> where T: UserInterfaceA, T: UserInterfaceB {}
    <!CONFLICTING_OVERLOADS!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) open fun <T> testTypeParameterWithMultipleTypeAliasedUpperBoundsAABReverse(arg: T)<!> where T: UserInterfaceA, T: SameUserInterfaceB {}

    <!CONFLICTING_OVERLOADS!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) open fun <T> testTypeParameterWithMultipleTypeAliasedUpperBoundsAAC(arg: Invariant<T>)<!> where T: UserInterfaceA, T: UserInterfaceB {}
    <!CONFLICTING_OVERLOADS!>open fun <T> testTypeParameterWithMultipleTypeAliasedUpperBoundsAAC(arg: Invariant<T>)<!> where T: UserInterfaceA, T: SameUserInterfaceB {}

    <!CONFLICTING_OVERLOADS!>open fun <T> testTypeParameterWithMultipleTypeAliasedUpperBoundsAACReverse(arg: Invariant<T>)<!> where T: UserInterfaceA, T: UserInterfaceB {}
    <!CONFLICTING_OVERLOADS!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) open fun <T> testTypeParameterWithMultipleTypeAliasedUpperBoundsAACReverse(arg: Invariant<T>)<!> where T: UserInterfaceA, T: SameUserInterfaceB {}

    <!CONFLICTING_OVERLOADS!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) open fun <T: UserInterfaceA> testTypeParameterWithMultipleTypeAliasedUpperBoundsABA()<!> where T: UserInterfaceB {}
    <!CONFLICTING_OVERLOADS!>open fun <T: UserInterfaceA> testTypeParameterWithMultipleTypeAliasedUpperBoundsABA()<!> where T: SameUserInterfaceB {}

    <!CONFLICTING_OVERLOADS!>open fun <T: UserInterfaceA> testTypeParameterWithMultipleTypeAliasedUpperBoundsABAReverse()<!> where T: UserInterfaceB {}
    <!CONFLICTING_OVERLOADS!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) open fun <T: UserInterfaceA> testTypeParameterWithMultipleTypeAliasedUpperBoundsABAReverse()<!> where T: SameUserInterfaceB {}

    <!CONFLICTING_OVERLOADS!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) open fun <T: UserInterfaceA> testTypeParameterWithMultipleTypeAliasedUpperBoundsABB(arg: T)<!> where T: UserInterfaceB {}
    <!CONFLICTING_OVERLOADS!>open fun <T: UserInterfaceA> testTypeParameterWithMultipleTypeAliasedUpperBoundsABB(arg: T)<!> where T: SameUserInterfaceB {}

    <!CONFLICTING_OVERLOADS!>open fun <T: UserInterfaceA> testTypeParameterWithMultipleTypeAliasedUpperBoundsABBReverse(arg: T)<!> where T: UserInterfaceB {}
    <!CONFLICTING_OVERLOADS!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) open fun <T: UserInterfaceA> testTypeParameterWithMultipleTypeAliasedUpperBoundsABBReverse(arg: T)<!> where T: SameUserInterfaceB {}

    <!CONFLICTING_OVERLOADS!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) open fun <T: UserInterfaceA> testTypeParameterWithMultipleTypeAliasedUpperBoundsABC(arg: Invariant<T>)<!> where T: UserInterfaceB {}
    <!CONFLICTING_OVERLOADS!>open fun <T: UserInterfaceA> testTypeParameterWithMultipleTypeAliasedUpperBoundsABC(arg: Invariant<T>)<!> where T: SameUserInterfaceB {}

    <!CONFLICTING_OVERLOADS!>open fun <T: UserInterfaceA> testTypeParameterWithMultipleTypeAliasedUpperBoundsABCReverse(arg: Invariant<T>)<!> where T: UserInterfaceB {}
    <!CONFLICTING_OVERLOADS!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) open fun <T: UserInterfaceA> testTypeParameterWithMultipleTypeAliasedUpperBoundsABCReverse(arg: Invariant<T>)<!> where T: SameUserInterfaceB {}

    <!CONFLICTING_OVERLOADS!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) open fun <T: UserInterfaceB> testTypeParameterWithMultipleTypeAliasedUpperBoundsACA()<!> where T: UserInterfaceA {}
    <!CONFLICTING_OVERLOADS!>open fun <T: SameUserInterfaceB> testTypeParameterWithMultipleTypeAliasedUpperBoundsACA()<!> where T: UserInterfaceA {}

    <!CONFLICTING_OVERLOADS!>open fun <T: UserInterfaceB> testTypeParameterWithMultipleTypeAliasedUpperBoundsACAReverse()<!> where T: UserInterfaceA {}
    <!CONFLICTING_OVERLOADS!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) open fun <T: SameUserInterfaceB> testTypeParameterWithMultipleTypeAliasedUpperBoundsACAReverse()<!> where T: UserInterfaceA {}

    <!CONFLICTING_OVERLOADS!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) open fun <T: UserInterfaceB> testTypeParameterWithMultipleTypeAliasedUpperBoundsACB(arg: T)<!> where T: UserInterfaceA {}
    <!CONFLICTING_OVERLOADS!>open fun <T: SameUserInterfaceB> testTypeParameterWithMultipleTypeAliasedUpperBoundsACB(arg: T)<!> where T: UserInterfaceA {}

    <!CONFLICTING_OVERLOADS!>open fun <T: UserInterfaceB> testTypeParameterWithMultipleTypeAliasedUpperBoundsACBReverse(arg: T)<!> where T: UserInterfaceA {}
    <!CONFLICTING_OVERLOADS!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) open fun <T: SameUserInterfaceB> testTypeParameterWithMultipleTypeAliasedUpperBoundsACBReverse(arg: T)<!> where T: UserInterfaceA {}

    <!CONFLICTING_OVERLOADS!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) open fun <T: UserInterfaceB> testTypeParameterWithMultipleTypeAliasedUpperBoundsACC(arg: Invariant<T>)<!> where T: UserInterfaceA {}
    <!CONFLICTING_OVERLOADS!>open fun <T: SameUserInterfaceB> testTypeParameterWithMultipleTypeAliasedUpperBoundsACC(arg: Invariant<T>)<!> where T: UserInterfaceA {}

    <!CONFLICTING_OVERLOADS!>open fun <T: UserInterfaceB> testTypeParameterWithMultipleTypeAliasedUpperBoundsACCReverse(arg: Invariant<T>)<!> where T: UserInterfaceA {}
    <!CONFLICTING_OVERLOADS!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) open fun <T: SameUserInterfaceB> testTypeParameterWithMultipleTypeAliasedUpperBoundsACCReverse(arg: Invariant<T>)<!> where T: UserInterfaceA {}

    <!CONFLICTING_OVERLOADS!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) open fun <T> testTypeParameterWithMultipleTypeAliasedUpperBoundsBAA()<!> where T: UserInterfaceA, T: UserInterfaceB {}
    <!CONFLICTING_OVERLOADS!>open fun <T> testTypeParameterWithMultipleTypeAliasedUpperBoundsBAA()<!> where T: SameUserInterfaceA, T: SameUserInterfaceB {}

    <!CONFLICTING_OVERLOADS!>open fun <T> testTypeParameterWithMultipleTypeAliasedUpperBoundsBAAReverse()<!> where T: UserInterfaceA, T: UserInterfaceB {}
    <!CONFLICTING_OVERLOADS!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) open fun <T> testTypeParameterWithMultipleTypeAliasedUpperBoundsBAAReverse()<!> where T: SameUserInterfaceA, T: SameUserInterfaceB {}

    <!CONFLICTING_OVERLOADS!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) open fun <T> testTypeParameterWithMultipleTypeAliasedUpperBoundsBAB(arg: T)<!> where T: UserInterfaceA, T: UserInterfaceB {}
    <!CONFLICTING_OVERLOADS!>open fun <T> testTypeParameterWithMultipleTypeAliasedUpperBoundsBAB(arg: T)<!> where T: SameUserInterfaceA, T: SameUserInterfaceB {}

    <!CONFLICTING_OVERLOADS!>open fun <T> testTypeParameterWithMultipleTypeAliasedUpperBoundsBABReverse(arg: T)<!> where T: UserInterfaceA, T: UserInterfaceB {}
    <!CONFLICTING_OVERLOADS!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) open fun <T> testTypeParameterWithMultipleTypeAliasedUpperBoundsBABReverse(arg: T)<!> where T: SameUserInterfaceA, T: SameUserInterfaceB {}

    <!CONFLICTING_OVERLOADS!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) open fun <T> testTypeParameterWithMultipleTypeAliasedUpperBoundsBAC(arg: Invariant<T>)<!> where T: UserInterfaceA, T: UserInterfaceB {}
    <!CONFLICTING_OVERLOADS!>open fun <T> testTypeParameterWithMultipleTypeAliasedUpperBoundsBAC(arg: Invariant<T>)<!> where T: SameUserInterfaceA, T: SameUserInterfaceB {}

    <!CONFLICTING_OVERLOADS!>open fun <T> testTypeParameterWithMultipleTypeAliasedUpperBoundsBACReverse(arg: Invariant<T>)<!> where T: UserInterfaceA, T: UserInterfaceB {}
    <!CONFLICTING_OVERLOADS!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) open fun <T> testTypeParameterWithMultipleTypeAliasedUpperBoundsBACReverse(arg: Invariant<T>)<!> where T: SameUserInterfaceA, T: SameUserInterfaceB {}

    <!CONFLICTING_OVERLOADS!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) open fun <T: UserInterfaceA> testTypeParameterWithMultipleTypeAliasedUpperBoundsBBA()<!> where T: UserInterfaceB {}
    <!CONFLICTING_OVERLOADS!>open fun <T: SameUserInterfaceA> testTypeParameterWithMultipleTypeAliasedUpperBoundsBBA()<!> where T: SameUserInterfaceB {}

    <!CONFLICTING_OVERLOADS!>open fun <T: UserInterfaceA> testTypeParameterWithMultipleTypeAliasedUpperBoundsBBAReverse()<!> where T: UserInterfaceB {}
    <!CONFLICTING_OVERLOADS!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) open fun <T: SameUserInterfaceA> testTypeParameterWithMultipleTypeAliasedUpperBoundsBBAReverse()<!> where T: SameUserInterfaceB {}

    <!CONFLICTING_OVERLOADS!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) open fun <T: UserInterfaceA> testTypeParameterWithMultipleTypeAliasedUpperBoundsBBB(arg: T)<!> where T: UserInterfaceB {}
    <!CONFLICTING_OVERLOADS!>open fun <T: SameUserInterfaceA> testTypeParameterWithMultipleTypeAliasedUpperBoundsBBB(arg: T)<!> where T: SameUserInterfaceB {}

    <!CONFLICTING_OVERLOADS!>open fun <T: UserInterfaceA> testTypeParameterWithMultipleTypeAliasedUpperBoundsBBBReverse(arg: T)<!> where T: UserInterfaceB {}
    <!CONFLICTING_OVERLOADS!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) open fun <T: SameUserInterfaceA> testTypeParameterWithMultipleTypeAliasedUpperBoundsBBBReverse(arg: T)<!> where T: SameUserInterfaceB {}

    <!CONFLICTING_OVERLOADS!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) open fun <T: UserInterfaceA> testTypeParameterWithMultipleTypeAliasedUpperBoundsBBC(arg: Invariant<T>)<!> where T: UserInterfaceB {}
    <!CONFLICTING_OVERLOADS!>open fun <T: SameUserInterfaceA> testTypeParameterWithMultipleTypeAliasedUpperBoundsBBC(arg: Invariant<T>)<!> where T: SameUserInterfaceB {}

    <!CONFLICTING_OVERLOADS!>open fun <T: UserInterfaceA> testTypeParameterWithMultipleTypeAliasedUpperBoundsBBCReverse(arg: Invariant<T>)<!> where T: UserInterfaceB {}
    <!CONFLICTING_OVERLOADS!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) open fun <T: SameUserInterfaceA> testTypeParameterWithMultipleTypeAliasedUpperBoundsBBCReverse(arg: Invariant<T>)<!> where T: SameUserInterfaceB {}

    <!CONFLICTING_OVERLOADS!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) open fun <T> testTypeParameterWithMultipleShuffledUpperBoundsAA()<!> where T: UserInterfaceA, T: UserInterfaceB {}
    <!CONFLICTING_OVERLOADS!>open fun <T> testTypeParameterWithMultipleShuffledUpperBoundsAA()<!> where T: UserInterfaceB, T: UserInterfaceA {}

    <!CONFLICTING_OVERLOADS!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) open fun <T> testTypeParameterWithMultipleShuffledUpperBoundsAB(arg: T)<!> where T: UserInterfaceA, T: UserInterfaceB {}
    <!CONFLICTING_OVERLOADS!>open fun <T> testTypeParameterWithMultipleShuffledUpperBoundsAB(arg: T)<!> where T: UserInterfaceB, T: UserInterfaceA {}

    <!CONFLICTING_OVERLOADS!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) open fun <T> testTypeParameterWithMultipleShuffledUpperBoundsAC(arg: Invariant<T>)<!> where T: UserInterfaceA, T: UserInterfaceB {}
    <!CONFLICTING_OVERLOADS!>open fun <T> testTypeParameterWithMultipleShuffledUpperBoundsAC(arg: Invariant<T>)<!> where T: UserInterfaceB, T: UserInterfaceA {}

    <!CONFLICTING_OVERLOADS!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) open fun <T: UserInterfaceA> testTypeParameterWithMultipleShuffledUpperBoundsBA()<!> where T: UserInterfaceB {}
    <!CONFLICTING_OVERLOADS!>open fun <T: UserInterfaceB> testTypeParameterWithMultipleShuffledUpperBoundsBA()<!> where T: UserInterfaceA {}

    <!CONFLICTING_OVERLOADS!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) open fun <T: UserInterfaceA> testTypeParameterWithMultipleShuffledUpperBoundsBB(arg: T)<!> where T: UserInterfaceB {}
    <!CONFLICTING_OVERLOADS!>open fun <T: UserInterfaceB> testTypeParameterWithMultipleShuffledUpperBoundsBB(arg: T)<!> where T: UserInterfaceA {}

    <!CONFLICTING_OVERLOADS!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) open fun <T: UserInterfaceA> testTypeParameterWithMultipleShuffledUpperBoundsBC(arg: Invariant<T>)<!> where T: UserInterfaceB {}
    <!CONFLICTING_OVERLOADS!>open fun <T: UserInterfaceB> testTypeParameterWithMultipleShuffledUpperBoundsBC(arg: Invariant<T>)<!> where T: UserInterfaceA {}

    <!CONFLICTING_OVERLOADS!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) infix open fun UserKlass.testIdenticalPresenceOfInfixModifier(arg: UserKlass)<!> {}
    <!CONFLICTING_OVERLOADS!>infix open fun UserKlass.testIdenticalPresenceOfInfixModifier(arg: UserKlass)<!> {}

    <!CONFLICTING_OVERLOADS!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) infix open fun UserKlass.testDifferencesInInfixModifierPresence(arg: UserKlass)<!> {}
    <!CONFLICTING_OVERLOADS!>open fun UserKlass.testDifferencesInInfixModifierPresence(arg: UserKlass)<!> {}

    <!CONFLICTING_OVERLOADS!>infix open fun UserKlass.testDifferencesInInfixModifierPresenceReverse(arg: UserKlass)<!> {}
    <!CONFLICTING_OVERLOADS!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) open fun UserKlass.testDifferencesInInfixModifierPresenceReverse(arg: UserKlass)<!> {}

    <!CONFLICTING_OVERLOADS!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) operator open fun UserKlassA.unaryPlus()<!> {}
    <!CONFLICTING_OVERLOADS!>operator open fun UserKlassA.unaryPlus()<!> {}

    <!CONFLICTING_OVERLOADS!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) operator open fun UserKlassB.unaryPlus()<!> {}
    <!CONFLICTING_OVERLOADS!>open fun UserKlassB.unaryPlus()<!> {}

    <!CONFLICTING_OVERLOADS!>operator open fun UserKlassB.unaryMinus()<!> {}
    <!CONFLICTING_OVERLOADS!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) open fun UserKlassB.unaryMinus()<!> {}

    <!CONFLICTING_OVERLOADS!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) internal open fun testIdenticalInternalVisibility()<!> {}
    <!CONFLICTING_OVERLOADS!>internal open fun testIdenticalInternalVisibility()<!> {}

    <!CONFLICTING_OVERLOADS!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) internal open fun testDifferencesInInternalAndPublicVisibilities()<!> {}
    <!CONFLICTING_OVERLOADS!>public open fun testDifferencesInInternalAndPublicVisibilities()<!> {}

    <!CONFLICTING_OVERLOADS!>internal open fun testDifferencesInInternalAndPublicVisibilitiesReverse()<!> {}
    <!CONFLICTING_OVERLOADS!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) public open fun testDifferencesInInternalAndPublicVisibilitiesReverse()<!> {}

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
