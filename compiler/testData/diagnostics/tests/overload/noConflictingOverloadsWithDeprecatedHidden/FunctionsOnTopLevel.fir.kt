// RUN_PIPELINE_TILL: BACKEND
// LATEST_LV_DIFFERENCE
// IGNORE_DEXING
// DIAGNOSTICS: -MISPLACED_TYPE_PARAMETER_CONSTRAINTS, -NOTHING_TO_INLINE, -NO_TAIL_CALLS_FOUND


@Deprecated(message = "", level = DeprecationLevel.HIDDEN) <!CONFLICTING_JVM_DECLARATIONS!>fun testBasic() {}<!>
<!CONFLICTING_JVM_DECLARATIONS!>fun testBasic() {}<!>


@Deprecated(message = "", level = DeprecationLevel.HIDDEN) <!CONFLICTING_JVM_DECLARATIONS!>fun testIdenticalReturnTypes(): UserKlass = UserKlass()<!>
<!CONFLICTING_JVM_DECLARATIONS!>fun testIdenticalReturnTypes(): UserKlass = UserKlass()<!>

@Deprecated(message = "", level = DeprecationLevel.HIDDEN) <!CONFLICTING_JVM_DECLARATIONS!>fun testDifferencesInReturnTypePresence(): Unit {}<!>
<!CONFLICTING_JVM_DECLARATIONS!>fun testDifferencesInReturnTypePresence() {}<!>

<!CONFLICTING_JVM_DECLARATIONS!>fun testDifferencesInReturnTypePresenceReverse(): Unit {}<!>
@Deprecated(message = "", level = DeprecationLevel.HIDDEN) <!CONFLICTING_JVM_DECLARATIONS!>fun testDifferencesInReturnTypePresenceReverse() {}<!>

@Deprecated(message = "", level = DeprecationLevel.HIDDEN) fun testDifferentReturnTypes(): UserKlassA = UserKlassA()
fun testDifferentReturnTypes(): UserKlassB = UserKlassB()

@Deprecated(message = "", level = DeprecationLevel.HIDDEN) <!CONFLICTING_JVM_DECLARATIONS!>fun testVarianceDifferentReturnTypesA(): Invariant<UserKlass> = Invariant()<!>
<!CONFLICTING_JVM_DECLARATIONS!>fun testVarianceDifferentReturnTypesA(): Invariant<out UserKlass> = Invariant()<!>

<!CONFLICTING_JVM_DECLARATIONS!>fun testVarianceDifferentReturnTypesAReverse(): Invariant<UserKlass> = Invariant()<!>
@Deprecated(message = "", level = DeprecationLevel.HIDDEN) <!CONFLICTING_JVM_DECLARATIONS!>fun testVarianceDifferentReturnTypesAReverse(): Invariant<out UserKlass> = Invariant()<!>

@Deprecated(message = "", level = DeprecationLevel.HIDDEN) <!CONFLICTING_JVM_DECLARATIONS!>fun testVarianceDifferentReturnTypesB(): Invariant<UserKlass> = Invariant()<!>
<!CONFLICTING_JVM_DECLARATIONS!>fun testVarianceDifferentReturnTypesB(): Invariant<in UserKlass> = Invariant()<!>

<!CONFLICTING_JVM_DECLARATIONS!>fun testVarianceDifferentReturnTypesBReverse(): Invariant<UserKlass> = Invariant()<!>
@Deprecated(message = "", level = DeprecationLevel.HIDDEN) <!CONFLICTING_JVM_DECLARATIONS!>fun testVarianceDifferentReturnTypesBReverse(): Invariant<in UserKlass> = Invariant()<!>

@Deprecated(message = "", level = DeprecationLevel.HIDDEN) <!CONFLICTING_JVM_DECLARATIONS!>fun testVarianceDifferentReturnTypesC(): Invariant<UserKlass> = Invariant()<!>
<!CONFLICTING_JVM_DECLARATIONS!>fun testVarianceDifferentReturnTypesC(): Invariant<*> = Invariant<UserKlass>()<!>

<!CONFLICTING_JVM_DECLARATIONS!>fun testVarianceDifferentReturnTypesCReverse(): Invariant<UserKlass> = Invariant()<!>
@Deprecated(message = "", level = DeprecationLevel.HIDDEN) <!CONFLICTING_JVM_DECLARATIONS!>fun testVarianceDifferentReturnTypesCReverse(): Invariant<*> = Invariant<UserKlass>()<!>

@Deprecated(message = "", level = DeprecationLevel.HIDDEN) <!CONFLICTING_JVM_DECLARATIONS!>fun testVarianceDifferentReturnTypesD(): Invariant<out UserKlass> = Invariant()<!>
<!CONFLICTING_JVM_DECLARATIONS!>fun testVarianceDifferentReturnTypesD(): Invariant<*> = Invariant<UserKlass>()<!>

<!CONFLICTING_JVM_DECLARATIONS!>fun testVarianceDifferentReturnTypesDReverse(): Invariant<out UserKlass> = Invariant()<!>
@Deprecated(message = "", level = DeprecationLevel.HIDDEN) <!CONFLICTING_JVM_DECLARATIONS!>fun testVarianceDifferentReturnTypesDReverse(): Invariant<*> = Invariant<UserKlass>()<!>

@Deprecated(message = "", level = DeprecationLevel.HIDDEN) <!CONFLICTING_JVM_DECLARATIONS!>fun testVarianceDifferentReturnTypesE(): Invariant<in UserKlass> = Invariant()<!>
<!CONFLICTING_JVM_DECLARATIONS!>fun testVarianceDifferentReturnTypesE(): Invariant<*> = Invariant<UserKlass>()<!>

<!CONFLICTING_JVM_DECLARATIONS!>fun testVarianceDifferentReturnTypesEReverse(): Invariant<in UserKlass> = Invariant()<!>
@Deprecated(message = "", level = DeprecationLevel.HIDDEN) <!CONFLICTING_JVM_DECLARATIONS!>fun testVarianceDifferentReturnTypesEReverse(): Invariant<*> = Invariant<UserKlass>()<!>

@Deprecated(message = "", level = DeprecationLevel.HIDDEN) <!CONFLICTING_JVM_DECLARATIONS!>fun testVarianceDifferentReturnTypesF(): Invariant<out UserKlass> = Invariant()<!>
<!CONFLICTING_JVM_DECLARATIONS!>fun testVarianceDifferentReturnTypesF(): Invariant<in UserKlass> = Invariant()<!>

<!CONFLICTING_JVM_DECLARATIONS!>fun testVarianceDifferentReturnTypesFReverse(): Invariant<out UserKlass> = Invariant()<!>
@Deprecated(message = "", level = DeprecationLevel.HIDDEN) <!CONFLICTING_JVM_DECLARATIONS!>fun testVarianceDifferentReturnTypesFReverse(): Invariant<in UserKlass> = Invariant()<!>

@Deprecated(message = "", level = DeprecationLevel.HIDDEN) <!CONFLICTING_JVM_DECLARATIONS!>fun testTypeAliasedReturnTypes(): UserKlass = UserKlass()<!>
<!CONFLICTING_JVM_DECLARATIONS!>fun testTypeAliasedReturnTypes(): SameUserKlass = UserKlass()<!>

<!CONFLICTING_JVM_DECLARATIONS!>fun testTypeAliasedReturnTypesReverse(): UserKlass = UserKlass()<!>
@Deprecated(message = "", level = DeprecationLevel.HIDDEN) <!CONFLICTING_JVM_DECLARATIONS!>fun testTypeAliasedReturnTypesReverse(): SameUserKlass = UserKlass()<!>


@Deprecated(message = "", level = DeprecationLevel.HIDDEN) <!CONFLICTING_JVM_DECLARATIONS!>fun UserKlass.testIdenticalExtensionReceivers() {}<!>
<!CONFLICTING_JVM_DECLARATIONS!>fun UserKlass.testIdenticalExtensionReceivers() {}<!>

@Deprecated(message = "", level = DeprecationLevel.HIDDEN) <!CONFLICTING_JVM_DECLARATIONS!>fun UserKlass.testTypeAliasedExtensionReceivers() {}<!>
<!CONFLICTING_JVM_DECLARATIONS!>fun SameUserKlass.testTypeAliasedExtensionReceivers() {}<!>

<!CONFLICTING_JVM_DECLARATIONS!>fun UserKlass.testTypeAliasedExtensionReceiversReverse() {}<!>
@Deprecated(message = "", level = DeprecationLevel.HIDDEN) <!CONFLICTING_JVM_DECLARATIONS!>fun SameUserKlass.testTypeAliasedExtensionReceiversReverse() {}<!>


@Deprecated(message = "", level = DeprecationLevel.HIDDEN) <!CONFLICTING_JVM_DECLARATIONS!>fun testIdenticalValueParameters(arg: UserKlass) {}<!>
<!CONFLICTING_JVM_DECLARATIONS!>fun testIdenticalValueParameters(arg: UserKlass) {}<!>

@Deprecated(message = "", level = DeprecationLevel.HIDDEN) <!CONFLICTING_JVM_DECLARATIONS!>fun testDifferentlyNamedValueParameters(argA: UserKlass) {}<!>
<!CONFLICTING_JVM_DECLARATIONS!>fun testDifferentlyNamedValueParameters(argB: UserKlass) {}<!>

@Deprecated(message = "", level = DeprecationLevel.HIDDEN) <!CONFLICTING_JVM_DECLARATIONS!>fun testTypeAliasedValueParameterTypes(arg: UserKlass) {}<!>
<!CONFLICTING_JVM_DECLARATIONS!>fun testTypeAliasedValueParameterTypes(arg: SameUserKlass) {}<!>

<!CONFLICTING_JVM_DECLARATIONS!>fun testTypeAliasedValueParameterTypesReverse(arg: UserKlass) {}<!>
@Deprecated(message = "", level = DeprecationLevel.HIDDEN) <!CONFLICTING_JVM_DECLARATIONS!>fun testTypeAliasedValueParameterTypesReverse(arg: SameUserKlass) {}<!>


@Deprecated(message = "", level = DeprecationLevel.HIDDEN) <!CONFLICTING_JVM_DECLARATIONS!>fun testMultipleIdenticalValueParameters(arg1: UserKlassA, arg2: UserKlassB) {}<!>
<!CONFLICTING_JVM_DECLARATIONS!>fun testMultipleIdenticalValueParameters(arg1: UserKlassA, arg2: UserKlassB) {}<!>

@Deprecated(message = "", level = DeprecationLevel.HIDDEN) <!CONFLICTING_JVM_DECLARATIONS!>fun testMultipleDifferentlyNamedValueParametersA(arg1: UserKlassA, arg2A: UserKlassB) {}<!>
<!CONFLICTING_JVM_DECLARATIONS!>fun testMultipleDifferentlyNamedValueParametersA(arg1: UserKlassA, arg2B: UserKlassB) {}<!>

@Deprecated(message = "", level = DeprecationLevel.HIDDEN) <!CONFLICTING_JVM_DECLARATIONS!>fun testMultipleDifferentlyNamedValueParametersB(arg1A: UserKlassA, arg2A: UserKlassB) {}<!>
<!CONFLICTING_JVM_DECLARATIONS!>fun testMultipleDifferentlyNamedValueParametersB(arg1B: UserKlassA, arg2B: UserKlassB) {}<!>

@Deprecated(message = "", level = DeprecationLevel.HIDDEN) <!CONFLICTING_JVM_DECLARATIONS!>fun testMultipleTypeAliasedValueParameterTypesA(arg1: UserKlassA, arg2: UserKlassB) {}<!>
<!CONFLICTING_JVM_DECLARATIONS!>fun testMultipleTypeAliasedValueParameterTypesA(arg1: UserKlassA, arg2: SameUserKlassB) {}<!>

<!CONFLICTING_JVM_DECLARATIONS!>fun testMultipleTypeAliasedValueParameterTypesAReverse(arg1: UserKlassA, arg2: UserKlassB) {}<!>
@Deprecated(message = "", level = DeprecationLevel.HIDDEN) <!CONFLICTING_JVM_DECLARATIONS!>fun testMultipleTypeAliasedValueParameterTypesAReverse(arg1: UserKlassA, arg2: SameUserKlassB) {}<!>

@Deprecated(message = "", level = DeprecationLevel.HIDDEN) <!CONFLICTING_JVM_DECLARATIONS!>fun testMultipleTypeAliasedValueParameterTypesB(arg1: UserKlassA, arg2: UserKlassB) {}<!>
<!CONFLICTING_JVM_DECLARATIONS!>fun testMultipleTypeAliasedValueParameterTypesB(arg1: SameUserKlassA, arg2: SameUserKlassB) {}<!>

<!CONFLICTING_JVM_DECLARATIONS!>fun testMultipleTypeAliasedValueParameterTypesBReverse(arg1: UserKlassA, arg2: UserKlassB) {}<!>
@Deprecated(message = "", level = DeprecationLevel.HIDDEN) <!CONFLICTING_JVM_DECLARATIONS!>fun testMultipleTypeAliasedValueParameterTypesBReverse(arg1: SameUserKlassA, arg2: SameUserKlassB) {}<!>


@Deprecated(message = "", level = DeprecationLevel.HIDDEN) <!CONFLICTING_JVM_DECLARATIONS!>fun testValueParameterWithIdenticalDefaultArguments(arg: UserKlass = defaultArgument) {}<!>
<!CONFLICTING_JVM_DECLARATIONS!>fun testValueParameterWithIdenticalDefaultArguments(arg: UserKlass = defaultArgument) {}<!>

@Deprecated(message = "", level = DeprecationLevel.HIDDEN) <!CONFLICTING_JVM_DECLARATIONS!>fun testDifferencesInValueParameterDefaultArgumentsPresence(arg: UserKlass = defaultArgument) {}<!>
<!CONFLICTING_JVM_DECLARATIONS!>fun testDifferencesInValueParameterDefaultArgumentsPresence(arg: UserKlass) {}<!>

<!CONFLICTING_JVM_DECLARATIONS!>fun testDifferencesInValueParameterDefaultArgumentsPresenceReverse(arg: UserKlass = defaultArgument) {}<!>
@Deprecated(message = "", level = DeprecationLevel.HIDDEN) <!CONFLICTING_JVM_DECLARATIONS!>fun testDifferencesInValueParameterDefaultArgumentsPresenceReverse(arg: UserKlass) {}<!>

@Deprecated(message = "", level = DeprecationLevel.HIDDEN) <!CONFLICTING_JVM_DECLARATIONS!>fun testValueParameterWithDifferentDefaultArguments(arg: UserKlass = defaultArgumentA) {}<!>
<!CONFLICTING_JVM_DECLARATIONS!>fun testValueParameterWithDifferentDefaultArguments(arg: UserKlass = defaultArgumentB) {}<!>

@Deprecated(message = "", level = DeprecationLevel.HIDDEN) <!CONFLICTING_JVM_DECLARATIONS!>fun testValueParameterWithAliasedDefaultArguments(arg: UserKlass = defaultArgument) {}<!>
<!CONFLICTING_JVM_DECLARATIONS!>fun testValueParameterWithAliasedDefaultArguments(arg: UserKlass = sameDefaultArgument) {}<!>

<!CONFLICTING_JVM_DECLARATIONS!>fun testValueParameterWithAliasedDefaultArgumentsReverse(arg: UserKlass = defaultArgument) {}<!>
@Deprecated(message = "", level = DeprecationLevel.HIDDEN) <!CONFLICTING_JVM_DECLARATIONS!>fun testValueParameterWithAliasedDefaultArgumentsReverse(arg: UserKlass = sameDefaultArgument) {}<!>


@Deprecated(message = "", level = DeprecationLevel.HIDDEN) <!CONFLICTING_JVM_DECLARATIONS!>fun <T> testIdenticalTypeParametersA() {}<!>
<!CONFLICTING_JVM_DECLARATIONS!>fun <T> testIdenticalTypeParametersA() {}<!>

@Deprecated(message = "", level = DeprecationLevel.HIDDEN) <!CONFLICTING_JVM_DECLARATIONS!>fun <T> testIdenticalTypeParametersB(arg: T) {}<!>
<!CONFLICTING_JVM_DECLARATIONS!>fun <T> testIdenticalTypeParametersB(arg: T) {}<!>

@Deprecated(message = "", level = DeprecationLevel.HIDDEN) <!CONFLICTING_JVM_DECLARATIONS!>fun <T> testIdenticalTypeParametersC(arg: Invariant<T>) {}<!>
<!CONFLICTING_JVM_DECLARATIONS!>fun <T> testIdenticalTypeParametersC(arg: Invariant<T>) {}<!>

@Deprecated(message = "", level = DeprecationLevel.HIDDEN) <!CONFLICTING_JVM_DECLARATIONS!>fun <TA> testDifferentlyNamedTypeParametersA() {}<!>
<!CONFLICTING_JVM_DECLARATIONS!>fun <TB> testDifferentlyNamedTypeParametersA() {}<!>

@Deprecated(message = "", level = DeprecationLevel.HIDDEN) <!CONFLICTING_JVM_DECLARATIONS!>fun <TA> testDifferentlyNamedTypeParametersB(arg: TA) {}<!>
<!CONFLICTING_JVM_DECLARATIONS!>fun <TB> testDifferentlyNamedTypeParametersB(arg: TB) {}<!>

@Deprecated(message = "", level = DeprecationLevel.HIDDEN) <!CONFLICTING_JVM_DECLARATIONS!>fun <TA> testDifferentlyNamedTypeParametersC(arg: Invariant<TA>) {}<!>
<!CONFLICTING_JVM_DECLARATIONS!>fun <TB> testDifferentlyNamedTypeParametersC(arg: Invariant<TB>) {}<!>


@Deprecated(message = "", level = DeprecationLevel.HIDDEN) <!CONFLICTING_JVM_DECLARATIONS!>fun <T1, T2> testMultipleIdenticalTypeParameters() {}<!>
<!CONFLICTING_JVM_DECLARATIONS!>fun <T1, T2> testMultipleIdenticalTypeParameters() {}<!>

@Deprecated(message = "", level = DeprecationLevel.HIDDEN) <!CONFLICTING_JVM_DECLARATIONS!>fun <T1, T2A> testMultipleDifferentlyNamedTypeParametersA() {}<!>
<!CONFLICTING_JVM_DECLARATIONS!>fun <T1, T2B> testMultipleDifferentlyNamedTypeParametersA() {}<!>

@Deprecated(message = "", level = DeprecationLevel.HIDDEN) <!CONFLICTING_JVM_DECLARATIONS!>fun <T1A, T2A> testMultipleDifferentlyNamedTypeParametersB() {}<!>
<!CONFLICTING_JVM_DECLARATIONS!>fun <T1B, T2B> testMultipleDifferentlyNamedTypeParametersB() {}<!>


@Deprecated(message = "", level = DeprecationLevel.HIDDEN) <!CONFLICTING_JVM_DECLARATIONS!>fun <T: UserInterface> testTypeParameterWithIdenticalUpperBoundsA() {}<!>
<!CONFLICTING_JVM_DECLARATIONS!>fun <T: UserInterface> testTypeParameterWithIdenticalUpperBoundsA() {}<!>

@Deprecated(message = "", level = DeprecationLevel.HIDDEN) <!CONFLICTING_JVM_DECLARATIONS!>fun <T: UserInterface> testTypeParameterWithIdenticalUpperBoundsB(arg: T) {}<!>
<!CONFLICTING_JVM_DECLARATIONS!>fun <T: UserInterface> testTypeParameterWithIdenticalUpperBoundsB(arg: T) {}<!>

@Deprecated(message = "", level = DeprecationLevel.HIDDEN) <!CONFLICTING_JVM_DECLARATIONS!>fun <T: UserInterface> testTypeParameterWithIdenticalUpperBoundsC(arg: Invariant<T>) {}<!>
<!CONFLICTING_JVM_DECLARATIONS!>fun <T: UserInterface> testTypeParameterWithIdenticalUpperBoundsC(arg: Invariant<T>) {}<!>

@Deprecated(message = "", level = DeprecationLevel.HIDDEN) <!CONFLICTING_JVM_DECLARATIONS!>fun <T: UserInterface> testDifferencesInTypeParameterUpperBoundsPresence() {}<!>
<!CONFLICTING_JVM_DECLARATIONS!>fun <T> testDifferencesInTypeParameterUpperBoundsPresence() {}<!>

<!CONFLICTING_JVM_DECLARATIONS!>fun <T: UserInterface> testDifferencesInTypeParameterUpperBoundsPresenceReverse() {}<!>
@Deprecated(message = "", level = DeprecationLevel.HIDDEN) <!CONFLICTING_JVM_DECLARATIONS!>fun <T> testDifferencesInTypeParameterUpperBoundsPresenceReverse() {}<!>

@Deprecated(message = "", level = DeprecationLevel.HIDDEN) <!CONFLICTING_JVM_DECLARATIONS!>fun <T: UserInterfaceA> testTypeParameterWithDifferentUpperBounds() {}<!>
<!CONFLICTING_JVM_DECLARATIONS!>fun <T: UserInterfaceB> testTypeParameterWithDifferentUpperBounds() {}<!>

@Deprecated(message = "", level = DeprecationLevel.HIDDEN) <!CONFLICTING_JVM_DECLARATIONS!>fun <T: Invariant<UserInterface>> testTypeParameterWithVarianceDifferentUpperBoundsA() {}<!>
<!CONFLICTING_JVM_DECLARATIONS!>fun <T: Invariant<out UserInterface>> testTypeParameterWithVarianceDifferentUpperBoundsA() {}<!>

<!CONFLICTING_JVM_DECLARATIONS!>fun <T: Invariant<UserInterface>> testTypeParameterWithVarianceDifferentUpperBoundsAReverse() {}<!>
@Deprecated(message = "", level = DeprecationLevel.HIDDEN) <!CONFLICTING_JVM_DECLARATIONS!>fun <T: Invariant<out UserInterface>> testTypeParameterWithVarianceDifferentUpperBoundsAReverse() {}<!>

@Deprecated(message = "", level = DeprecationLevel.HIDDEN) <!CONFLICTING_JVM_DECLARATIONS!>fun <T: Invariant<UserInterface>> testTypeParameterWithVarianceDifferentUpperBoundsB() {}<!>
<!CONFLICTING_JVM_DECLARATIONS!>fun <T: Invariant<in UserInterface>> testTypeParameterWithVarianceDifferentUpperBoundsB() {}<!>

<!CONFLICTING_JVM_DECLARATIONS!>fun <T: Invariant<UserInterface>> testTypeParameterWithVarianceDifferentUpperBoundsBReverse() {}<!>
@Deprecated(message = "", level = DeprecationLevel.HIDDEN) <!CONFLICTING_JVM_DECLARATIONS!>fun <T: Invariant<in UserInterface>> testTypeParameterWithVarianceDifferentUpperBoundsBReverse() {}<!>

@Deprecated(message = "", level = DeprecationLevel.HIDDEN) <!CONFLICTING_JVM_DECLARATIONS!>fun <T: Invariant<UserInterface>> testTypeParameterWithVarianceDifferentUpperBoundsC() {}<!>
<!CONFLICTING_JVM_DECLARATIONS!>fun <T: Invariant<*>> testTypeParameterWithVarianceDifferentUpperBoundsC() {}<!>

<!CONFLICTING_JVM_DECLARATIONS!>fun <T: Invariant<UserInterface>> testTypeParameterWithVarianceDifferentUpperBoundsCReverse() {}<!>
@Deprecated(message = "", level = DeprecationLevel.HIDDEN) <!CONFLICTING_JVM_DECLARATIONS!>fun <T: Invariant<*>> testTypeParameterWithVarianceDifferentUpperBoundsCReverse() {}<!>

@Deprecated(message = "", level = DeprecationLevel.HIDDEN) <!CONFLICTING_JVM_DECLARATIONS!>fun <T: Invariant<out UserInterface>> testTypeParameterWithVarianceDifferentUpperBoundsD() {}<!>
<!CONFLICTING_JVM_DECLARATIONS!>fun <T: Invariant<*>> testTypeParameterWithVarianceDifferentUpperBoundsD() {}<!>

<!CONFLICTING_JVM_DECLARATIONS!>fun <T: Invariant<out UserInterface>> testTypeParameterWithVarianceDifferentUpperBoundsDReverse() {}<!>
@Deprecated(message = "", level = DeprecationLevel.HIDDEN) <!CONFLICTING_JVM_DECLARATIONS!>fun <T: Invariant<*>> testTypeParameterWithVarianceDifferentUpperBoundsDReverse() {}<!>

@Deprecated(message = "", level = DeprecationLevel.HIDDEN) <!CONFLICTING_JVM_DECLARATIONS!>fun <T: Invariant<in UserInterface>> testTypeParameterWithVarianceDifferentUpperBoundsE() {}<!>
<!CONFLICTING_JVM_DECLARATIONS!>fun <T: Invariant<*>> testTypeParameterWithVarianceDifferentUpperBoundsE() {}<!>

<!CONFLICTING_JVM_DECLARATIONS!>fun <T: Invariant<in UserInterface>> testTypeParameterWithVarianceDifferentUpperBoundsEReverse() {}<!>
@Deprecated(message = "", level = DeprecationLevel.HIDDEN) <!CONFLICTING_JVM_DECLARATIONS!>fun <T: Invariant<*>> testTypeParameterWithVarianceDifferentUpperBoundsEReverse() {}<!>

@Deprecated(message = "", level = DeprecationLevel.HIDDEN) <!CONFLICTING_JVM_DECLARATIONS!>fun <T: Invariant<out UserInterface>> testTypeParameterWithVarianceDifferentUpperBoundsF() {}<!>
<!CONFLICTING_JVM_DECLARATIONS!>fun <T: Invariant<in UserInterface>> testTypeParameterWithVarianceDifferentUpperBoundsF() {}<!>

<!CONFLICTING_JVM_DECLARATIONS!>fun <T: Invariant<out UserInterface>> testTypeParameterWithVarianceDifferentUpperBoundsFReverse() {}<!>
@Deprecated(message = "", level = DeprecationLevel.HIDDEN) <!CONFLICTING_JVM_DECLARATIONS!>fun <T: Invariant<in UserInterface>> testTypeParameterWithVarianceDifferentUpperBoundsFReverse() {}<!>

@Deprecated(message = "", level = DeprecationLevel.HIDDEN) <!CONFLICTING_JVM_DECLARATIONS!>fun <T: UserInterface> testTypeParameterWithTypeAliasedUpperBoundsA() {}<!>
<!CONFLICTING_JVM_DECLARATIONS!>fun <T: SameUserInterface> testTypeParameterWithTypeAliasedUpperBoundsA() {}<!>

<!CONFLICTING_JVM_DECLARATIONS!>fun <T: UserInterface> testTypeParameterWithTypeAliasedUpperBoundsAReverse() {}<!>
@Deprecated(message = "", level = DeprecationLevel.HIDDEN) <!CONFLICTING_JVM_DECLARATIONS!>fun <T: SameUserInterface> testTypeParameterWithTypeAliasedUpperBoundsAReverse() {}<!>

@Deprecated(message = "", level = DeprecationLevel.HIDDEN) <!CONFLICTING_JVM_DECLARATIONS!>fun <T: UserInterface> testTypeParameterWithTypeAliasedUpperBoundsB(arg: T) {}<!>
<!CONFLICTING_JVM_DECLARATIONS!>fun <T: SameUserInterface> testTypeParameterWithTypeAliasedUpperBoundsB(arg: T) {}<!>

<!CONFLICTING_JVM_DECLARATIONS!>fun <T: UserInterface> testTypeParameterWithTypeAliasedUpperBoundsBReverse(arg: T) {}<!>
@Deprecated(message = "", level = DeprecationLevel.HIDDEN) <!CONFLICTING_JVM_DECLARATIONS!>fun <T: SameUserInterface> testTypeParameterWithTypeAliasedUpperBoundsBReverse(arg: T) {}<!>

@Deprecated(message = "", level = DeprecationLevel.HIDDEN) <!CONFLICTING_JVM_DECLARATIONS!>fun <T: UserInterface> testTypeParameterWithTypeAliasedUpperBoundsC(arg: Invariant<T>) {}<!>
<!CONFLICTING_JVM_DECLARATIONS!>fun <T: SameUserInterface> testTypeParameterWithTypeAliasedUpperBoundsC(arg: Invariant<T>) {}<!>

<!CONFLICTING_JVM_DECLARATIONS!>fun <T: UserInterface> testTypeParameterWithTypeAliasedUpperBoundsCReverse(arg: Invariant<T>) {}<!>
@Deprecated(message = "", level = DeprecationLevel.HIDDEN) <!CONFLICTING_JVM_DECLARATIONS!>fun <T: SameUserInterface> testTypeParameterWithTypeAliasedUpperBoundsCReverse(arg: Invariant<T>) {}<!>


@Deprecated(message = "", level = DeprecationLevel.HIDDEN) <!CONFLICTING_JVM_DECLARATIONS!>fun <T> testTypeParameterWithMultipleIdenticalUpperBoundsAA() where T: UserInterfaceA, T: UserInterfaceB {}<!>
<!CONFLICTING_JVM_DECLARATIONS!>fun <T> testTypeParameterWithMultipleIdenticalUpperBoundsAA() where T: UserInterfaceA, T: UserInterfaceB {}<!>

@Deprecated(message = "", level = DeprecationLevel.HIDDEN) <!CONFLICTING_JVM_DECLARATIONS!>fun <T> testTypeParameterWithMultipleIdenticalUpperBoundsAB(arg: T) where T: UserInterfaceA, T: UserInterfaceB {}<!>
<!CONFLICTING_JVM_DECLARATIONS!>fun <T> testTypeParameterWithMultipleIdenticalUpperBoundsAB(arg: T) where T: UserInterfaceA, T: UserInterfaceB {}<!>

@Deprecated(message = "", level = DeprecationLevel.HIDDEN) <!CONFLICTING_JVM_DECLARATIONS!>fun <T> testTypeParameterWithMultipleIdenticalUpperBoundsAC(arg: Invariant<T>) where T: UserInterfaceA, T: UserInterfaceB {}<!>
<!CONFLICTING_JVM_DECLARATIONS!>fun <T> testTypeParameterWithMultipleIdenticalUpperBoundsAC(arg: Invariant<T>) where T: UserInterfaceA, T: UserInterfaceB {}<!>

@Deprecated(message = "", level = DeprecationLevel.HIDDEN) <!CONFLICTING_JVM_DECLARATIONS!>fun <T: UserInterfaceA> testTypeParameterWithMultipleIdenticalUpperBoundsBA() where T: UserInterfaceB {}<!>
<!CONFLICTING_JVM_DECLARATIONS!>fun <T: UserInterfaceA> testTypeParameterWithMultipleIdenticalUpperBoundsBA() where T: UserInterfaceB {}<!>

@Deprecated(message = "", level = DeprecationLevel.HIDDEN) <!CONFLICTING_JVM_DECLARATIONS!>fun <T: UserInterfaceA> testTypeParameterWithMultipleIdenticalUpperBoundsBB(arg: T) where T: UserInterfaceB {}<!>
<!CONFLICTING_JVM_DECLARATIONS!>fun <T: UserInterfaceA> testTypeParameterWithMultipleIdenticalUpperBoundsBB(arg: T) where T: UserInterfaceB {}<!>

@Deprecated(message = "", level = DeprecationLevel.HIDDEN) <!CONFLICTING_JVM_DECLARATIONS!>fun <T: UserInterfaceA> testTypeParameterWithMultipleIdenticalUpperBoundsBC(arg: Invariant<T>) where T: UserInterfaceB {}<!>
<!CONFLICTING_JVM_DECLARATIONS!>fun <T: UserInterfaceA> testTypeParameterWithMultipleIdenticalUpperBoundsBC(arg: Invariant<T>) where T: UserInterfaceB {}<!>

@Deprecated(message = "", level = DeprecationLevel.HIDDEN) <!CONFLICTING_JVM_DECLARATIONS!>fun <T> testDifferencesInTypeParameterMultipleUpperBoundsPresenceA() where T: UserInterfaceA, T: UserInterfaceB {}<!>
<!CONFLICTING_JVM_DECLARATIONS!>fun <T> testDifferencesInTypeParameterMultipleUpperBoundsPresenceA() where T: UserInterfaceA {}<!>

<!CONFLICTING_JVM_DECLARATIONS!>fun <T: UserInterfaceA> testDifferencesInTypeParameterMultipleUpperBoundsPresenceB() where T: UserInterfaceB {}<!>
@Deprecated(message = "", level = DeprecationLevel.HIDDEN) <!CONFLICTING_JVM_DECLARATIONS!>fun <T: UserInterfaceA> testDifferencesInTypeParameterMultipleUpperBoundsPresenceB() {}<!>

@Deprecated(message = "", level = DeprecationLevel.HIDDEN) <!CONFLICTING_JVM_DECLARATIONS!>fun <T> testTypeParameterWithMultipleDifferentUpperBoundsAA() where T: UserInterfaceA, T: UserInterfaceB {}<!>
<!CONFLICTING_JVM_DECLARATIONS!>fun <T> testTypeParameterWithMultipleDifferentUpperBoundsAA() where T: UserInterfaceA, T: UserInterfaceC {}<!>

@Deprecated(message = "", level = DeprecationLevel.HIDDEN) <!CONFLICTING_JVM_DECLARATIONS!>fun <T: UserInterfaceA> testTypeParameterWithMultipleDifferentUpperBoundsAB() where T: UserInterfaceB {}<!>
<!CONFLICTING_JVM_DECLARATIONS!>fun <T: UserInterfaceA> testTypeParameterWithMultipleDifferentUpperBoundsAB() where T: UserInterfaceC {}<!>

@Deprecated(message = "", level = DeprecationLevel.HIDDEN) <!CONFLICTING_JVM_DECLARATIONS!>fun <T: UserInterfaceB> testTypeParameterWithMultipleDifferentUpperBoundsAC() where T: UserInterfaceA {}<!>
<!CONFLICTING_JVM_DECLARATIONS!>fun <T: UserInterfaceC> testTypeParameterWithMultipleDifferentUpperBoundsAC() where T: UserInterfaceA {}<!>

@Deprecated(message = "", level = DeprecationLevel.HIDDEN) <!CONFLICTING_JVM_DECLARATIONS!>fun <T> testTypeParameterWithMultipleDifferentUpperBoundsBA() where T: UserInterfaceA, T: UserInterfaceB {}<!>
<!CONFLICTING_JVM_DECLARATIONS!>fun <T> testTypeParameterWithMultipleDifferentUpperBoundsBA() where T: UserInterfaceC, T: UserInterfaceD {}<!>

@Deprecated(message = "", level = DeprecationLevel.HIDDEN) <!CONFLICTING_JVM_DECLARATIONS!>fun <T: UserInterfaceA> testTypeParameterWithMultipleDifferentUpperBoundsBB() where T: UserInterfaceB {}<!>
<!CONFLICTING_JVM_DECLARATIONS!>fun <T: UserInterfaceC> testTypeParameterWithMultipleDifferentUpperBoundsBB() where T: UserInterfaceD {}<!>

@Deprecated(message = "", level = DeprecationLevel.HIDDEN) <!CONFLICTING_JVM_DECLARATIONS!>fun <T> testTypeParameterWithMultipleTypeAliasedUpperBoundsAAA() where T: UserInterfaceA, T: UserInterfaceB {}<!>
<!CONFLICTING_JVM_DECLARATIONS!>fun <T> testTypeParameterWithMultipleTypeAliasedUpperBoundsAAA() where T: UserInterfaceA, T: SameUserInterfaceB {}<!>

<!CONFLICTING_JVM_DECLARATIONS!>fun <T> testTypeParameterWithMultipleTypeAliasedUpperBoundsAAAReverse() where T: UserInterfaceA, T: UserInterfaceB {}<!>
@Deprecated(message = "", level = DeprecationLevel.HIDDEN) <!CONFLICTING_JVM_DECLARATIONS!>fun <T> testTypeParameterWithMultipleTypeAliasedUpperBoundsAAAReverse() where T: UserInterfaceA, T: SameUserInterfaceB {}<!>

@Deprecated(message = "", level = DeprecationLevel.HIDDEN) <!CONFLICTING_JVM_DECLARATIONS!>fun <T> testTypeParameterWithMultipleTypeAliasedUpperBoundsAAB(arg: T) where T: UserInterfaceA, T: UserInterfaceB {}<!>
<!CONFLICTING_JVM_DECLARATIONS!>fun <T> testTypeParameterWithMultipleTypeAliasedUpperBoundsAAB(arg: T) where T: UserInterfaceA, T: SameUserInterfaceB {}<!>

<!CONFLICTING_JVM_DECLARATIONS!>fun <T> testTypeParameterWithMultipleTypeAliasedUpperBoundsAABReverse(arg: T) where T: UserInterfaceA, T: UserInterfaceB {}<!>
@Deprecated(message = "", level = DeprecationLevel.HIDDEN) <!CONFLICTING_JVM_DECLARATIONS!>fun <T> testTypeParameterWithMultipleTypeAliasedUpperBoundsAABReverse(arg: T) where T: UserInterfaceA, T: SameUserInterfaceB {}<!>

@Deprecated(message = "", level = DeprecationLevel.HIDDEN) <!CONFLICTING_JVM_DECLARATIONS!>fun <T> testTypeParameterWithMultipleTypeAliasedUpperBoundsAAC(arg: Invariant<T>) where T: UserInterfaceA, T: UserInterfaceB {}<!>
<!CONFLICTING_JVM_DECLARATIONS!>fun <T> testTypeParameterWithMultipleTypeAliasedUpperBoundsAAC(arg: Invariant<T>) where T: UserInterfaceA, T: SameUserInterfaceB {}<!>

<!CONFLICTING_JVM_DECLARATIONS!>fun <T> testTypeParameterWithMultipleTypeAliasedUpperBoundsAACReverse(arg: Invariant<T>) where T: UserInterfaceA, T: UserInterfaceB {}<!>
@Deprecated(message = "", level = DeprecationLevel.HIDDEN) <!CONFLICTING_JVM_DECLARATIONS!>fun <T> testTypeParameterWithMultipleTypeAliasedUpperBoundsAACReverse(arg: Invariant<T>) where T: UserInterfaceA, T: SameUserInterfaceB {}<!>

@Deprecated(message = "", level = DeprecationLevel.HIDDEN) <!CONFLICTING_JVM_DECLARATIONS!>fun <T: UserInterfaceA> testTypeParameterWithMultipleTypeAliasedUpperBoundsABA() where T: UserInterfaceB {}<!>
<!CONFLICTING_JVM_DECLARATIONS!>fun <T: UserInterfaceA> testTypeParameterWithMultipleTypeAliasedUpperBoundsABA() where T: SameUserInterfaceB {}<!>

<!CONFLICTING_JVM_DECLARATIONS!>fun <T: UserInterfaceA> testTypeParameterWithMultipleTypeAliasedUpperBoundsABAReverse() where T: UserInterfaceB {}<!>
@Deprecated(message = "", level = DeprecationLevel.HIDDEN) <!CONFLICTING_JVM_DECLARATIONS!>fun <T: UserInterfaceA> testTypeParameterWithMultipleTypeAliasedUpperBoundsABAReverse() where T: SameUserInterfaceB {}<!>

@Deprecated(message = "", level = DeprecationLevel.HIDDEN) <!CONFLICTING_JVM_DECLARATIONS!>fun <T: UserInterfaceA> testTypeParameterWithMultipleTypeAliasedUpperBoundsABB(arg: T) where T: UserInterfaceB {}<!>
<!CONFLICTING_JVM_DECLARATIONS!>fun <T: UserInterfaceA> testTypeParameterWithMultipleTypeAliasedUpperBoundsABB(arg: T) where T: SameUserInterfaceB {}<!>

<!CONFLICTING_JVM_DECLARATIONS!>fun <T: UserInterfaceA> testTypeParameterWithMultipleTypeAliasedUpperBoundsABBReverse(arg: T) where T: UserInterfaceB {}<!>
@Deprecated(message = "", level = DeprecationLevel.HIDDEN) <!CONFLICTING_JVM_DECLARATIONS!>fun <T: UserInterfaceA> testTypeParameterWithMultipleTypeAliasedUpperBoundsABBReverse(arg: T) where T: SameUserInterfaceB {}<!>

@Deprecated(message = "", level = DeprecationLevel.HIDDEN) <!CONFLICTING_JVM_DECLARATIONS!>fun <T: UserInterfaceA> testTypeParameterWithMultipleTypeAliasedUpperBoundsABC(arg: Invariant<T>) where T: UserInterfaceB {}<!>
<!CONFLICTING_JVM_DECLARATIONS!>fun <T: UserInterfaceA> testTypeParameterWithMultipleTypeAliasedUpperBoundsABC(arg: Invariant<T>) where T: SameUserInterfaceB {}<!>

<!CONFLICTING_JVM_DECLARATIONS!>fun <T: UserInterfaceA> testTypeParameterWithMultipleTypeAliasedUpperBoundsABCReverse(arg: Invariant<T>) where T: UserInterfaceB {}<!>
@Deprecated(message = "", level = DeprecationLevel.HIDDEN) <!CONFLICTING_JVM_DECLARATIONS!>fun <T: UserInterfaceA> testTypeParameterWithMultipleTypeAliasedUpperBoundsABCReverse(arg: Invariant<T>) where T: SameUserInterfaceB {}<!>

@Deprecated(message = "", level = DeprecationLevel.HIDDEN) <!CONFLICTING_JVM_DECLARATIONS!>fun <T: UserInterfaceB> testTypeParameterWithMultipleTypeAliasedUpperBoundsACA() where T: UserInterfaceA {}<!>
<!CONFLICTING_JVM_DECLARATIONS!>fun <T: SameUserInterfaceB> testTypeParameterWithMultipleTypeAliasedUpperBoundsACA() where T: UserInterfaceA {}<!>

<!CONFLICTING_JVM_DECLARATIONS!>fun <T: UserInterfaceB> testTypeParameterWithMultipleTypeAliasedUpperBoundsACAReverse() where T: UserInterfaceA {}<!>
@Deprecated(message = "", level = DeprecationLevel.HIDDEN) <!CONFLICTING_JVM_DECLARATIONS!>fun <T: SameUserInterfaceB> testTypeParameterWithMultipleTypeAliasedUpperBoundsACAReverse() where T: UserInterfaceA {}<!>

@Deprecated(message = "", level = DeprecationLevel.HIDDEN) <!CONFLICTING_JVM_DECLARATIONS!>fun <T: UserInterfaceB> testTypeParameterWithMultipleTypeAliasedUpperBoundsACB(arg: T) where T: UserInterfaceA {}<!>
<!CONFLICTING_JVM_DECLARATIONS!>fun <T: SameUserInterfaceB> testTypeParameterWithMultipleTypeAliasedUpperBoundsACB(arg: T) where T: UserInterfaceA {}<!>

<!CONFLICTING_JVM_DECLARATIONS!>fun <T: UserInterfaceB> testTypeParameterWithMultipleTypeAliasedUpperBoundsACBReverse(arg: T) where T: UserInterfaceA {}<!>
@Deprecated(message = "", level = DeprecationLevel.HIDDEN) <!CONFLICTING_JVM_DECLARATIONS!>fun <T: SameUserInterfaceB> testTypeParameterWithMultipleTypeAliasedUpperBoundsACBReverse(arg: T) where T: UserInterfaceA {}<!>

@Deprecated(message = "", level = DeprecationLevel.HIDDEN) <!CONFLICTING_JVM_DECLARATIONS!>fun <T: UserInterfaceB> testTypeParameterWithMultipleTypeAliasedUpperBoundsACC(arg: Invariant<T>) where T: UserInterfaceA {}<!>
<!CONFLICTING_JVM_DECLARATIONS!>fun <T: SameUserInterfaceB> testTypeParameterWithMultipleTypeAliasedUpperBoundsACC(arg: Invariant<T>) where T: UserInterfaceA {}<!>

<!CONFLICTING_JVM_DECLARATIONS!>fun <T: UserInterfaceB> testTypeParameterWithMultipleTypeAliasedUpperBoundsACCReverse(arg: Invariant<T>) where T: UserInterfaceA {}<!>
@Deprecated(message = "", level = DeprecationLevel.HIDDEN) <!CONFLICTING_JVM_DECLARATIONS!>fun <T: SameUserInterfaceB> testTypeParameterWithMultipleTypeAliasedUpperBoundsACCReverse(arg: Invariant<T>) where T: UserInterfaceA {}<!>

@Deprecated(message = "", level = DeprecationLevel.HIDDEN) <!CONFLICTING_JVM_DECLARATIONS!>fun <T> testTypeParameterWithMultipleTypeAliasedUpperBoundsBAA() where T: UserInterfaceA, T: UserInterfaceB {}<!>
<!CONFLICTING_JVM_DECLARATIONS!>fun <T> testTypeParameterWithMultipleTypeAliasedUpperBoundsBAA() where T: SameUserInterfaceA, T: SameUserInterfaceB {}<!>

<!CONFLICTING_JVM_DECLARATIONS!>fun <T> testTypeParameterWithMultipleTypeAliasedUpperBoundsBAAReverse() where T: UserInterfaceA, T: UserInterfaceB {}<!>
@Deprecated(message = "", level = DeprecationLevel.HIDDEN) <!CONFLICTING_JVM_DECLARATIONS!>fun <T> testTypeParameterWithMultipleTypeAliasedUpperBoundsBAAReverse() where T: SameUserInterfaceA, T: SameUserInterfaceB {}<!>

@Deprecated(message = "", level = DeprecationLevel.HIDDEN) <!CONFLICTING_JVM_DECLARATIONS!>fun <T> testTypeParameterWithMultipleTypeAliasedUpperBoundsBAB(arg: T) where T: UserInterfaceA, T: UserInterfaceB {}<!>
<!CONFLICTING_JVM_DECLARATIONS!>fun <T> testTypeParameterWithMultipleTypeAliasedUpperBoundsBAB(arg: T) where T: SameUserInterfaceA, T: SameUserInterfaceB {}<!>

<!CONFLICTING_JVM_DECLARATIONS!>fun <T> testTypeParameterWithMultipleTypeAliasedUpperBoundsBABReverse(arg: T) where T: UserInterfaceA, T: UserInterfaceB {}<!>
@Deprecated(message = "", level = DeprecationLevel.HIDDEN) <!CONFLICTING_JVM_DECLARATIONS!>fun <T> testTypeParameterWithMultipleTypeAliasedUpperBoundsBABReverse(arg: T) where T: SameUserInterfaceA, T: SameUserInterfaceB {}<!>

@Deprecated(message = "", level = DeprecationLevel.HIDDEN) <!CONFLICTING_JVM_DECLARATIONS!>fun <T> testTypeParameterWithMultipleTypeAliasedUpperBoundsBAC(arg: Invariant<T>) where T: UserInterfaceA, T: UserInterfaceB {}<!>
<!CONFLICTING_JVM_DECLARATIONS!>fun <T> testTypeParameterWithMultipleTypeAliasedUpperBoundsBAC(arg: Invariant<T>) where T: SameUserInterfaceA, T: SameUserInterfaceB {}<!>

<!CONFLICTING_JVM_DECLARATIONS!>fun <T> testTypeParameterWithMultipleTypeAliasedUpperBoundsBACReverse(arg: Invariant<T>) where T: UserInterfaceA, T: UserInterfaceB {}<!>
@Deprecated(message = "", level = DeprecationLevel.HIDDEN) <!CONFLICTING_JVM_DECLARATIONS!>fun <T> testTypeParameterWithMultipleTypeAliasedUpperBoundsBACReverse(arg: Invariant<T>) where T: SameUserInterfaceA, T: SameUserInterfaceB {}<!>

@Deprecated(message = "", level = DeprecationLevel.HIDDEN) <!CONFLICTING_JVM_DECLARATIONS!>fun <T: UserInterfaceA> testTypeParameterWithMultipleTypeAliasedUpperBoundsBBA() where T: UserInterfaceB {}<!>
<!CONFLICTING_JVM_DECLARATIONS!>fun <T: SameUserInterfaceA> testTypeParameterWithMultipleTypeAliasedUpperBoundsBBA() where T: SameUserInterfaceB {}<!>

<!CONFLICTING_JVM_DECLARATIONS!>fun <T: UserInterfaceA> testTypeParameterWithMultipleTypeAliasedUpperBoundsBBAReverse() where T: UserInterfaceB {}<!>
@Deprecated(message = "", level = DeprecationLevel.HIDDEN) <!CONFLICTING_JVM_DECLARATIONS!>fun <T: SameUserInterfaceA> testTypeParameterWithMultipleTypeAliasedUpperBoundsBBAReverse() where T: SameUserInterfaceB {}<!>

@Deprecated(message = "", level = DeprecationLevel.HIDDEN) <!CONFLICTING_JVM_DECLARATIONS!>fun <T: UserInterfaceA> testTypeParameterWithMultipleTypeAliasedUpperBoundsBBB(arg: T) where T: UserInterfaceB {}<!>
<!CONFLICTING_JVM_DECLARATIONS!>fun <T: SameUserInterfaceA> testTypeParameterWithMultipleTypeAliasedUpperBoundsBBB(arg: T) where T: SameUserInterfaceB {}<!>

<!CONFLICTING_JVM_DECLARATIONS!>fun <T: UserInterfaceA> testTypeParameterWithMultipleTypeAliasedUpperBoundsBBBReverse(arg: T) where T: UserInterfaceB {}<!>
@Deprecated(message = "", level = DeprecationLevel.HIDDEN) <!CONFLICTING_JVM_DECLARATIONS!>fun <T: SameUserInterfaceA> testTypeParameterWithMultipleTypeAliasedUpperBoundsBBBReverse(arg: T) where T: SameUserInterfaceB {}<!>

@Deprecated(message = "", level = DeprecationLevel.HIDDEN) <!CONFLICTING_JVM_DECLARATIONS!>fun <T: UserInterfaceA> testTypeParameterWithMultipleTypeAliasedUpperBoundsBBC(arg: Invariant<T>) where T: UserInterfaceB {}<!>
<!CONFLICTING_JVM_DECLARATIONS!>fun <T: SameUserInterfaceA> testTypeParameterWithMultipleTypeAliasedUpperBoundsBBC(arg: Invariant<T>) where T: SameUserInterfaceB {}<!>

<!CONFLICTING_JVM_DECLARATIONS!>fun <T: UserInterfaceA> testTypeParameterWithMultipleTypeAliasedUpperBoundsBBCReverse(arg: Invariant<T>) where T: UserInterfaceB {}<!>
@Deprecated(message = "", level = DeprecationLevel.HIDDEN) <!CONFLICTING_JVM_DECLARATIONS!>fun <T: SameUserInterfaceA> testTypeParameterWithMultipleTypeAliasedUpperBoundsBBCReverse(arg: Invariant<T>) where T: SameUserInterfaceB {}<!>

@Deprecated(message = "", level = DeprecationLevel.HIDDEN) <!CONFLICTING_JVM_DECLARATIONS!>fun <T> testTypeParameterWithMultipleShuffledUpperBoundsAA() where T: UserInterfaceA, T: UserInterfaceB {}<!>
<!CONFLICTING_JVM_DECLARATIONS!>fun <T> testTypeParameterWithMultipleShuffledUpperBoundsAA() where T: UserInterfaceB, T: UserInterfaceA {}<!>

@Deprecated(message = "", level = DeprecationLevel.HIDDEN) fun <T> testTypeParameterWithMultipleShuffledUpperBoundsAB(arg: T) where T: UserInterfaceA, T: UserInterfaceB {}
fun <T> testTypeParameterWithMultipleShuffledUpperBoundsAB(arg: T) where T: UserInterfaceB, T: UserInterfaceA {}

@Deprecated(message = "", level = DeprecationLevel.HIDDEN) <!CONFLICTING_JVM_DECLARATIONS!>fun <T> testTypeParameterWithMultipleShuffledUpperBoundsAC(arg: Invariant<T>) where T: UserInterfaceA, T: UserInterfaceB {}<!>
<!CONFLICTING_JVM_DECLARATIONS!>fun <T> testTypeParameterWithMultipleShuffledUpperBoundsAC(arg: Invariant<T>) where T: UserInterfaceB, T: UserInterfaceA {}<!>

@Deprecated(message = "", level = DeprecationLevel.HIDDEN) <!CONFLICTING_JVM_DECLARATIONS!>fun <T: UserInterfaceA> testTypeParameterWithMultipleShuffledUpperBoundsBA() where T: UserInterfaceB {}<!>
<!CONFLICTING_JVM_DECLARATIONS!>fun <T: UserInterfaceB> testTypeParameterWithMultipleShuffledUpperBoundsBA() where T: UserInterfaceA {}<!>

@Deprecated(message = "", level = DeprecationLevel.HIDDEN) fun <T: UserInterfaceA> testTypeParameterWithMultipleShuffledUpperBoundsBB(arg: T) where T: UserInterfaceB {}
fun <T: UserInterfaceB> testTypeParameterWithMultipleShuffledUpperBoundsBB(arg: T) where T: UserInterfaceA {}

@Deprecated(message = "", level = DeprecationLevel.HIDDEN) <!CONFLICTING_JVM_DECLARATIONS!>fun <T: UserInterfaceA> testTypeParameterWithMultipleShuffledUpperBoundsBC(arg: Invariant<T>) where T: UserInterfaceB {}<!>
<!CONFLICTING_JVM_DECLARATIONS!>fun <T: UserInterfaceB> testTypeParameterWithMultipleShuffledUpperBoundsBC(arg: Invariant<T>) where T: UserInterfaceA {}<!>


@Deprecated(message = "", level = DeprecationLevel.HIDDEN) inline <!CONFLICTING_JVM_DECLARATIONS!>fun <reified T> testIdenticalReifiedTypeParameterA() {}<!>
inline <!CONFLICTING_JVM_DECLARATIONS!>fun <reified T> testIdenticalReifiedTypeParameterA() {}<!>

@Deprecated(message = "", level = DeprecationLevel.HIDDEN) inline <!CONFLICTING_JVM_DECLARATIONS!>fun <reified T> testIdenticalReifiedTypeParameterB(arg: T) {}<!>
inline <!CONFLICTING_JVM_DECLARATIONS!>fun <reified T> testIdenticalReifiedTypeParameterB(arg: T) {}<!>

@Deprecated(message = "", level = DeprecationLevel.HIDDEN) inline <!CONFLICTING_JVM_DECLARATIONS!>fun <reified T> testIdenticalReifiedTypeParameterC(arg: Invariant<T>) {}<!>
inline <!CONFLICTING_JVM_DECLARATIONS!>fun <reified T> testIdenticalReifiedTypeParameterC(arg: Invariant<T>) {}<!>

@Deprecated(message = "", level = DeprecationLevel.HIDDEN) inline <!CONFLICTING_JVM_DECLARATIONS!>fun <reified T> testDifferencesInReifiedBehaviorOfTypeParameterA() {}<!>
inline <!CONFLICTING_JVM_DECLARATIONS!>fun <T> testDifferencesInReifiedBehaviorOfTypeParameterA() {}<!>

inline <!CONFLICTING_JVM_DECLARATIONS!>fun <reified T> testDifferencesInReifiedBehaviorOfTypeParameterAReverse() {}<!>
@Deprecated(message = "", level = DeprecationLevel.HIDDEN) inline <!CONFLICTING_JVM_DECLARATIONS!>fun <T> testDifferencesInReifiedBehaviorOfTypeParameterAReverse() {}<!>

@Deprecated(message = "", level = DeprecationLevel.HIDDEN) inline <!CONFLICTING_JVM_DECLARATIONS!>fun <reified T> testDifferencesInReifiedBehaviorOfTypeParameterB(arg: T) {}<!>
inline <!CONFLICTING_JVM_DECLARATIONS!>fun <T> testDifferencesInReifiedBehaviorOfTypeParameterB(arg: T) {}<!>

inline <!CONFLICTING_JVM_DECLARATIONS!>fun <reified T> testDifferencesInReifiedBehaviorOfTypeParameterBReverse(arg: T) {}<!>
@Deprecated(message = "", level = DeprecationLevel.HIDDEN) inline <!CONFLICTING_JVM_DECLARATIONS!>fun <T> testDifferencesInReifiedBehaviorOfTypeParameterBReverse(arg: T) {}<!>

@Deprecated(message = "", level = DeprecationLevel.HIDDEN) inline <!CONFLICTING_JVM_DECLARATIONS!>fun <reified T> testDifferencesInReifiedBehaviorOfTypeParameterC(arg: Invariant<T>) {}<!>
inline <!CONFLICTING_JVM_DECLARATIONS!>fun <T> testDifferencesInReifiedBehaviorOfTypeParameterC(arg: Invariant<T>) {}<!>

inline <!CONFLICTING_JVM_DECLARATIONS!>fun <reified T> testDifferencesInReifiedBehaviorOfTypeParameterCReverse(arg: Invariant<T>) {}<!>
@Deprecated(message = "", level = DeprecationLevel.HIDDEN) inline <!CONFLICTING_JVM_DECLARATIONS!>fun <T> testDifferencesInReifiedBehaviorOfTypeParameterCReverse(arg: Invariant<T>) {}<!>


@Deprecated(message = "", level = DeprecationLevel.HIDDEN) inline <!CONFLICTING_JVM_DECLARATIONS!>fun testIdenticalPresenceOfInlineModifier() {}<!>
inline <!CONFLICTING_JVM_DECLARATIONS!>fun testIdenticalPresenceOfInlineModifier() {}<!>

@Deprecated(message = "", level = DeprecationLevel.HIDDEN) inline <!CONFLICTING_JVM_DECLARATIONS!>fun testDifferencesInInlineModifierPresence() {}<!>
<!CONFLICTING_JVM_DECLARATIONS!>fun testDifferencesInInlineModifierPresence() {}<!>

inline <!CONFLICTING_JVM_DECLARATIONS!>fun testDifferencesInInlineModifierPresenceReverse() {}<!>
@Deprecated(message = "", level = DeprecationLevel.HIDDEN) <!CONFLICTING_JVM_DECLARATIONS!>fun testDifferencesInInlineModifierPresenceReverse() {}<!>


@Deprecated(message = "", level = DeprecationLevel.HIDDEN) infix <!CONFLICTING_JVM_DECLARATIONS!>fun UserKlass.testIdenticalPresenceOfInfixModifier(arg: UserKlass) {}<!>
infix <!CONFLICTING_JVM_DECLARATIONS!>fun UserKlass.testIdenticalPresenceOfInfixModifier(arg: UserKlass) {}<!>

@Deprecated(message = "", level = DeprecationLevel.HIDDEN) infix <!CONFLICTING_JVM_DECLARATIONS!>fun UserKlass.testDifferencesInInfixModifierPresence(arg: UserKlass) {}<!>
<!CONFLICTING_JVM_DECLARATIONS!>fun UserKlass.testDifferencesInInfixModifierPresence(arg: UserKlass) {}<!>

infix <!CONFLICTING_JVM_DECLARATIONS!>fun UserKlass.testDifferencesInInfixModifierPresenceReverse(arg: UserKlass) {}<!>
@Deprecated(message = "", level = DeprecationLevel.HIDDEN) <!CONFLICTING_JVM_DECLARATIONS!>fun UserKlass.testDifferencesInInfixModifierPresenceReverse(arg: UserKlass) {}<!>


@Deprecated(message = "", level = DeprecationLevel.HIDDEN) operator <!CONFLICTING_JVM_DECLARATIONS!>fun UserKlassA.unaryPlus() {}<!>
operator <!CONFLICTING_JVM_DECLARATIONS!>fun UserKlassA.unaryPlus() {}<!>

@Deprecated(message = "", level = DeprecationLevel.HIDDEN) operator <!CONFLICTING_JVM_DECLARATIONS!>fun UserKlassB.unaryPlus() {}<!>
<!CONFLICTING_JVM_DECLARATIONS!>fun UserKlassB.unaryPlus() {}<!>

operator <!CONFLICTING_JVM_DECLARATIONS!>fun UserKlassB.unaryMinus() {}<!>
@Deprecated(message = "", level = DeprecationLevel.HIDDEN) <!CONFLICTING_JVM_DECLARATIONS!>fun UserKlassB.unaryMinus() {}<!>


@Deprecated(message = "", level = DeprecationLevel.HIDDEN) tailrec <!CONFLICTING_JVM_DECLARATIONS!>fun testIdenticalPresenceOfTailrecModifier() {}<!>
tailrec <!CONFLICTING_JVM_DECLARATIONS!>fun testIdenticalPresenceOfTailrecModifier() {}<!>

@Deprecated(message = "", level = DeprecationLevel.HIDDEN) tailrec <!CONFLICTING_JVM_DECLARATIONS!>fun testDifferencesInTailrecModifierPresence() {}<!>
<!CONFLICTING_JVM_DECLARATIONS!>fun testDifferencesInTailrecModifierPresence() {}<!>

tailrec <!CONFLICTING_JVM_DECLARATIONS!>fun testDifferencesInTailrecModifierPresenceReverse() {}<!>
@Deprecated(message = "", level = DeprecationLevel.HIDDEN) <!CONFLICTING_JVM_DECLARATIONS!>fun testDifferencesInTailrecModifierPresenceReverse() {}<!>


@Deprecated(message = "", level = DeprecationLevel.HIDDEN) private <!CONFLICTING_JVM_DECLARATIONS!>fun testIdenticalPrivateVisibility() {}<!>
private <!CONFLICTING_JVM_DECLARATIONS!>fun testIdenticalPrivateVisibility() {}<!>

@Deprecated(message = "", level = DeprecationLevel.HIDDEN) internal <!CONFLICTING_JVM_DECLARATIONS!>fun testIdenticalInternalVisibility() {}<!>
internal <!CONFLICTING_JVM_DECLARATIONS!>fun testIdenticalInternalVisibility() {}<!>

@Deprecated(message = "", level = DeprecationLevel.HIDDEN) private <!CONFLICTING_JVM_DECLARATIONS!>fun testDifferencesInPrivateAndPublicVisibilities() {}<!>
public <!CONFLICTING_JVM_DECLARATIONS!>fun testDifferencesInPrivateAndPublicVisibilities() {}<!>

private <!CONFLICTING_JVM_DECLARATIONS!>fun testDifferencesInPrivateAndPublicVisibilitiesReverse() {}<!>
@Deprecated(message = "", level = DeprecationLevel.HIDDEN) public <!CONFLICTING_JVM_DECLARATIONS!>fun testDifferencesInPrivateAndPublicVisibilitiesReverse() {}<!>

@Deprecated(message = "", level = DeprecationLevel.HIDDEN) internal <!CONFLICTING_JVM_DECLARATIONS!>fun testDifferencesInInternalAndPublicVisibilities() {}<!>
public <!CONFLICTING_JVM_DECLARATIONS!>fun testDifferencesInInternalAndPublicVisibilities() {}<!>

internal <!CONFLICTING_JVM_DECLARATIONS!>fun testDifferencesInInternalAndPublicVisibilitiesReverse() {}<!>
@Deprecated(message = "", level = DeprecationLevel.HIDDEN) public <!CONFLICTING_JVM_DECLARATIONS!>fun testDifferencesInInternalAndPublicVisibilitiesReverse() {}<!>

@Deprecated(message = "", level = DeprecationLevel.HIDDEN) private <!CONFLICTING_JVM_DECLARATIONS!>fun testDifferencesInPrivateAndInternalVisibilities() {}<!>
internal <!CONFLICTING_JVM_DECLARATIONS!>fun testDifferencesInPrivateAndInternalVisibilities() {}<!>

private <!CONFLICTING_JVM_DECLARATIONS!>fun testDifferencesInPrivateAndInternalVisibilitiesReverse() {}<!>
@Deprecated(message = "", level = DeprecationLevel.HIDDEN) internal <!CONFLICTING_JVM_DECLARATIONS!>fun testDifferencesInPrivateAndInternalVisibilitiesReverse() {}<!>


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

/* GENERATED_FIR_TAGS: classDeclaration, funWithExtensionReceiver, functionDeclaration, inProjection, infix, inline,
interfaceDeclaration, nullableType, operator, outProjection, propertyDeclaration, reified, starProjection, stringLiteral,
tailrec, typeAliasDeclaration, typeConstraint, typeParameter */
