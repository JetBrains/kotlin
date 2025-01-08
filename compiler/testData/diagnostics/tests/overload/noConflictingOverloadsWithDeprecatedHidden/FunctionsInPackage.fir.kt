// RUN_PIPELINE_TILL: BACKEND
// LATEST_LV_DIFFERENCE
// IGNORE_DEXING
// DIAGNOSTICS: -MISPLACED_TYPE_PARAMETER_CONSTRAINTS, -NOTHING_TO_INLINE, -NO_TAIL_CALLS_FOUND

package pkg


<!CONFLICTING_JVM_DECLARATIONS!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) fun testBasic() {}<!>
<!CONFLICTING_JVM_DECLARATIONS!>fun testBasic() {}<!>


<!CONFLICTING_JVM_DECLARATIONS!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) fun testIdenticalReturnTypes(): UserKlass = UserKlass()<!>
<!CONFLICTING_JVM_DECLARATIONS!>fun testIdenticalReturnTypes(): UserKlass = UserKlass()<!>

<!CONFLICTING_JVM_DECLARATIONS!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) fun testDifferencesInReturnTypePresence(): Unit {}<!>
<!CONFLICTING_JVM_DECLARATIONS!>fun testDifferencesInReturnTypePresence() {}<!>

<!CONFLICTING_JVM_DECLARATIONS!>fun testDifferencesInReturnTypePresenceReverse(): Unit {}<!>
<!CONFLICTING_JVM_DECLARATIONS!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) fun testDifferencesInReturnTypePresenceReverse() {}<!>

@Deprecated(message = "", level = DeprecationLevel.HIDDEN) fun testDifferentReturnTypes(): UserKlassA = UserKlassA()
fun testDifferentReturnTypes(): UserKlassB = UserKlassB()

<!CONFLICTING_JVM_DECLARATIONS!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) fun testVarianceDifferentReturnTypesA(): Invariant<UserKlass> = Invariant()<!>
<!CONFLICTING_JVM_DECLARATIONS!>fun testVarianceDifferentReturnTypesA(): Invariant<out UserKlass> = Invariant()<!>

<!CONFLICTING_JVM_DECLARATIONS!>fun testVarianceDifferentReturnTypesAReverse(): Invariant<UserKlass> = Invariant()<!>
<!CONFLICTING_JVM_DECLARATIONS!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) fun testVarianceDifferentReturnTypesAReverse(): Invariant<out UserKlass> = Invariant()<!>

<!CONFLICTING_JVM_DECLARATIONS!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) fun testVarianceDifferentReturnTypesB(): Invariant<UserKlass> = Invariant()<!>
<!CONFLICTING_JVM_DECLARATIONS!>fun testVarianceDifferentReturnTypesB(): Invariant<in UserKlass> = Invariant()<!>

<!CONFLICTING_JVM_DECLARATIONS!>fun testVarianceDifferentReturnTypesBReverse(): Invariant<UserKlass> = Invariant()<!>
<!CONFLICTING_JVM_DECLARATIONS!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) fun testVarianceDifferentReturnTypesBReverse(): Invariant<in UserKlass> = Invariant()<!>

<!CONFLICTING_JVM_DECLARATIONS!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) fun testVarianceDifferentReturnTypesC(): Invariant<UserKlass> = Invariant()<!>
<!CONFLICTING_JVM_DECLARATIONS!>fun testVarianceDifferentReturnTypesC(): Invariant<*> = Invariant<UserKlass>()<!>

<!CONFLICTING_JVM_DECLARATIONS!>fun testVarianceDifferentReturnTypesCReverse(): Invariant<UserKlass> = Invariant()<!>
<!CONFLICTING_JVM_DECLARATIONS!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) fun testVarianceDifferentReturnTypesCReverse(): Invariant<*> = Invariant<UserKlass>()<!>

<!CONFLICTING_JVM_DECLARATIONS!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) fun testVarianceDifferentReturnTypesD(): Invariant<out UserKlass> = Invariant()<!>
<!CONFLICTING_JVM_DECLARATIONS!>fun testVarianceDifferentReturnTypesD(): Invariant<*> = Invariant<UserKlass>()<!>

<!CONFLICTING_JVM_DECLARATIONS!>fun testVarianceDifferentReturnTypesDReverse(): Invariant<out UserKlass> = Invariant()<!>
<!CONFLICTING_JVM_DECLARATIONS!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) fun testVarianceDifferentReturnTypesDReverse(): Invariant<*> = Invariant<UserKlass>()<!>

<!CONFLICTING_JVM_DECLARATIONS!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) fun testVarianceDifferentReturnTypesE(): Invariant<in UserKlass> = Invariant()<!>
<!CONFLICTING_JVM_DECLARATIONS!>fun testVarianceDifferentReturnTypesE(): Invariant<*> = Invariant<UserKlass>()<!>

<!CONFLICTING_JVM_DECLARATIONS!>fun testVarianceDifferentReturnTypesEReverse(): Invariant<in UserKlass> = Invariant()<!>
<!CONFLICTING_JVM_DECLARATIONS!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) fun testVarianceDifferentReturnTypesEReverse(): Invariant<*> = Invariant<UserKlass>()<!>

<!CONFLICTING_JVM_DECLARATIONS!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) fun testVarianceDifferentReturnTypesF(): Invariant<out UserKlass> = Invariant()<!>
<!CONFLICTING_JVM_DECLARATIONS!>fun testVarianceDifferentReturnTypesF(): Invariant<in UserKlass> = Invariant()<!>

<!CONFLICTING_JVM_DECLARATIONS!>fun testVarianceDifferentReturnTypesFReverse(): Invariant<out UserKlass> = Invariant()<!>
<!CONFLICTING_JVM_DECLARATIONS!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) fun testVarianceDifferentReturnTypesFReverse(): Invariant<in UserKlass> = Invariant()<!>

<!CONFLICTING_JVM_DECLARATIONS!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) fun testTypeAliasedReturnTypes(): UserKlass = UserKlass()<!>
<!CONFLICTING_JVM_DECLARATIONS!>fun testTypeAliasedReturnTypes(): SameUserKlass = UserKlass()<!>

<!CONFLICTING_JVM_DECLARATIONS!>fun testTypeAliasedReturnTypesReverse(): UserKlass = UserKlass()<!>
<!CONFLICTING_JVM_DECLARATIONS!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) fun testTypeAliasedReturnTypesReverse(): SameUserKlass = UserKlass()<!>


<!CONFLICTING_JVM_DECLARATIONS!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) fun UserKlass.testIdenticalExtensionReceivers() {}<!>
<!CONFLICTING_JVM_DECLARATIONS!>fun UserKlass.testIdenticalExtensionReceivers() {}<!>

<!CONFLICTING_JVM_DECLARATIONS!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) fun UserKlass.testTypeAliasedExtensionReceivers() {}<!>
<!CONFLICTING_JVM_DECLARATIONS!>fun SameUserKlass.testTypeAliasedExtensionReceivers() {}<!>

<!CONFLICTING_JVM_DECLARATIONS!>fun UserKlass.testTypeAliasedExtensionReceiversReverse() {}<!>
<!CONFLICTING_JVM_DECLARATIONS!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) fun SameUserKlass.testTypeAliasedExtensionReceiversReverse() {}<!>


<!CONFLICTING_JVM_DECLARATIONS!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) fun testIdenticalValueParameters(arg: UserKlass) {}<!>
<!CONFLICTING_JVM_DECLARATIONS!>fun testIdenticalValueParameters(arg: UserKlass) {}<!>

<!CONFLICTING_JVM_DECLARATIONS!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) fun testDifferentlyNamedValueParameters(argA: UserKlass) {}<!>
<!CONFLICTING_JVM_DECLARATIONS!>fun testDifferentlyNamedValueParameters(argB: UserKlass) {}<!>

<!CONFLICTING_JVM_DECLARATIONS!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) fun testTypeAliasedValueParameterTypes(arg: UserKlass) {}<!>
<!CONFLICTING_JVM_DECLARATIONS!>fun testTypeAliasedValueParameterTypes(arg: SameUserKlass) {}<!>

<!CONFLICTING_JVM_DECLARATIONS!>fun testTypeAliasedValueParameterTypesReverse(arg: UserKlass) {}<!>
<!CONFLICTING_JVM_DECLARATIONS!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) fun testTypeAliasedValueParameterTypesReverse(arg: SameUserKlass) {}<!>


<!CONFLICTING_JVM_DECLARATIONS!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) fun testMultipleIdenticalValueParameters(arg1: UserKlassA, arg2: UserKlassB) {}<!>
<!CONFLICTING_JVM_DECLARATIONS!>fun testMultipleIdenticalValueParameters(arg1: UserKlassA, arg2: UserKlassB) {}<!>

<!CONFLICTING_JVM_DECLARATIONS!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) fun testMultipleDifferentlyNamedValueParametersA(arg1: UserKlassA, arg2A: UserKlassB) {}<!>
<!CONFLICTING_JVM_DECLARATIONS!>fun testMultipleDifferentlyNamedValueParametersA(arg1: UserKlassA, arg2B: UserKlassB) {}<!>

<!CONFLICTING_JVM_DECLARATIONS!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) fun testMultipleDifferentlyNamedValueParametersB(arg1A: UserKlassA, arg2A: UserKlassB) {}<!>
<!CONFLICTING_JVM_DECLARATIONS!>fun testMultipleDifferentlyNamedValueParametersB(arg1B: UserKlassA, arg2B: UserKlassB) {}<!>

<!CONFLICTING_JVM_DECLARATIONS!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) fun testMultipleTypeAliasedValueParameterTypesA(arg1: UserKlassA, arg2: UserKlassB) {}<!>
<!CONFLICTING_JVM_DECLARATIONS!>fun testMultipleTypeAliasedValueParameterTypesA(arg1: UserKlassA, arg2: SameUserKlassB) {}<!>

<!CONFLICTING_JVM_DECLARATIONS!>fun testMultipleTypeAliasedValueParameterTypesAReverse(arg1: UserKlassA, arg2: UserKlassB) {}<!>
<!CONFLICTING_JVM_DECLARATIONS!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) fun testMultipleTypeAliasedValueParameterTypesAReverse(arg1: UserKlassA, arg2: SameUserKlassB) {}<!>

<!CONFLICTING_JVM_DECLARATIONS!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) fun testMultipleTypeAliasedValueParameterTypesB(arg1: UserKlassA, arg2: UserKlassB) {}<!>
<!CONFLICTING_JVM_DECLARATIONS!>fun testMultipleTypeAliasedValueParameterTypesB(arg1: SameUserKlassA, arg2: SameUserKlassB) {}<!>

<!CONFLICTING_JVM_DECLARATIONS!>fun testMultipleTypeAliasedValueParameterTypesBReverse(arg1: UserKlassA, arg2: UserKlassB) {}<!>
<!CONFLICTING_JVM_DECLARATIONS!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) fun testMultipleTypeAliasedValueParameterTypesBReverse(arg1: SameUserKlassA, arg2: SameUserKlassB) {}<!>


<!CONFLICTING_JVM_DECLARATIONS, CONFLICTING_JVM_DECLARATIONS!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) fun testValueParameterWithIdenticalDefaultArguments(arg: UserKlass = defaultArgument) {}<!>
<!CONFLICTING_JVM_DECLARATIONS, CONFLICTING_JVM_DECLARATIONS!>fun testValueParameterWithIdenticalDefaultArguments(arg: UserKlass = defaultArgument) {}<!>

<!CONFLICTING_JVM_DECLARATIONS!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) fun testDifferencesInValueParameterDefaultArgumentsPresence(arg: UserKlass = defaultArgument) {}<!>
<!CONFLICTING_JVM_DECLARATIONS!>fun testDifferencesInValueParameterDefaultArgumentsPresence(arg: UserKlass) {}<!>

<!CONFLICTING_JVM_DECLARATIONS!>fun testDifferencesInValueParameterDefaultArgumentsPresenceReverse(arg: UserKlass = defaultArgument) {}<!>
<!CONFLICTING_JVM_DECLARATIONS!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) fun testDifferencesInValueParameterDefaultArgumentsPresenceReverse(arg: UserKlass) {}<!>

<!CONFLICTING_JVM_DECLARATIONS, CONFLICTING_JVM_DECLARATIONS!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) fun testValueParameterWithDifferentDefaultArguments(arg: UserKlass = defaultArgumentA) {}<!>
<!CONFLICTING_JVM_DECLARATIONS, CONFLICTING_JVM_DECLARATIONS!>fun testValueParameterWithDifferentDefaultArguments(arg: UserKlass = defaultArgumentB) {}<!>

<!CONFLICTING_JVM_DECLARATIONS, CONFLICTING_JVM_DECLARATIONS!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) fun testValueParameterWithAliasedDefaultArguments(arg: UserKlass = defaultArgument) {}<!>
<!CONFLICTING_JVM_DECLARATIONS, CONFLICTING_JVM_DECLARATIONS!>fun testValueParameterWithAliasedDefaultArguments(arg: UserKlass = sameDefaultArgument) {}<!>

<!CONFLICTING_JVM_DECLARATIONS, CONFLICTING_JVM_DECLARATIONS!>fun testValueParameterWithAliasedDefaultArgumentsReverse(arg: UserKlass = defaultArgument) {}<!>
<!CONFLICTING_JVM_DECLARATIONS, CONFLICTING_JVM_DECLARATIONS!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) fun testValueParameterWithAliasedDefaultArgumentsReverse(arg: UserKlass = sameDefaultArgument) {}<!>


<!CONFLICTING_JVM_DECLARATIONS!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) fun <T> testIdenticalTypeParametersA() {}<!>
<!CONFLICTING_JVM_DECLARATIONS!>fun <T> testIdenticalTypeParametersA() {}<!>

<!CONFLICTING_JVM_DECLARATIONS!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) fun <T> testIdenticalTypeParametersB(arg: T) {}<!>
<!CONFLICTING_JVM_DECLARATIONS!>fun <T> testIdenticalTypeParametersB(arg: T) {}<!>

<!CONFLICTING_JVM_DECLARATIONS!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) fun <T> testIdenticalTypeParametersC(arg: Invariant<T>) {}<!>
<!CONFLICTING_JVM_DECLARATIONS!>fun <T> testIdenticalTypeParametersC(arg: Invariant<T>) {}<!>

<!CONFLICTING_JVM_DECLARATIONS!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) fun <TA> testDifferentlyNamedTypeParametersA() {}<!>
<!CONFLICTING_JVM_DECLARATIONS!>fun <TB> testDifferentlyNamedTypeParametersA() {}<!>

<!CONFLICTING_JVM_DECLARATIONS!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) fun <TA> testDifferentlyNamedTypeParametersB(arg: TA) {}<!>
<!CONFLICTING_JVM_DECLARATIONS!>fun <TB> testDifferentlyNamedTypeParametersB(arg: TB) {}<!>

<!CONFLICTING_JVM_DECLARATIONS!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) fun <TA> testDifferentlyNamedTypeParametersC(arg: Invariant<TA>) {}<!>
<!CONFLICTING_JVM_DECLARATIONS!>fun <TB> testDifferentlyNamedTypeParametersC(arg: Invariant<TB>) {}<!>


<!CONFLICTING_JVM_DECLARATIONS!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) fun <T1, T2> testMultipleIdenticalTypeParameters() {}<!>
<!CONFLICTING_JVM_DECLARATIONS!>fun <T1, T2> testMultipleIdenticalTypeParameters() {}<!>

<!CONFLICTING_JVM_DECLARATIONS!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) fun <T1, T2A> testMultipleDifferentlyNamedTypeParametersA() {}<!>
<!CONFLICTING_JVM_DECLARATIONS!>fun <T1, T2B> testMultipleDifferentlyNamedTypeParametersA() {}<!>

<!CONFLICTING_JVM_DECLARATIONS!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) fun <T1A, T2A> testMultipleDifferentlyNamedTypeParametersB() {}<!>
<!CONFLICTING_JVM_DECLARATIONS!>fun <T1B, T2B> testMultipleDifferentlyNamedTypeParametersB() {}<!>


<!CONFLICTING_JVM_DECLARATIONS!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) fun <T: UserInterface> testTypeParameterWithIdenticalUpperBoundsA() {}<!>
<!CONFLICTING_JVM_DECLARATIONS!>fun <T: UserInterface> testTypeParameterWithIdenticalUpperBoundsA() {}<!>

<!CONFLICTING_JVM_DECLARATIONS!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) fun <T: UserInterface> testTypeParameterWithIdenticalUpperBoundsB(arg: T) {}<!>
<!CONFLICTING_JVM_DECLARATIONS!>fun <T: UserInterface> testTypeParameterWithIdenticalUpperBoundsB(arg: T) {}<!>

<!CONFLICTING_JVM_DECLARATIONS!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) fun <T: UserInterface> testTypeParameterWithIdenticalUpperBoundsC(arg: Invariant<T>) {}<!>
<!CONFLICTING_JVM_DECLARATIONS!>fun <T: UserInterface> testTypeParameterWithIdenticalUpperBoundsC(arg: Invariant<T>) {}<!>

<!CONFLICTING_JVM_DECLARATIONS!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) fun <T: UserInterface> testDifferencesInTypeParameterUpperBoundsPresence() {}<!>
<!CONFLICTING_JVM_DECLARATIONS!>fun <T> testDifferencesInTypeParameterUpperBoundsPresence() {}<!>

<!CONFLICTING_JVM_DECLARATIONS!>fun <T: UserInterface> testDifferencesInTypeParameterUpperBoundsPresenceReverse() {}<!>
<!CONFLICTING_JVM_DECLARATIONS!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) fun <T> testDifferencesInTypeParameterUpperBoundsPresenceReverse() {}<!>

<!CONFLICTING_JVM_DECLARATIONS!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) fun <T: UserInterfaceA> testTypeParameterWithDifferentUpperBounds() {}<!>
<!CONFLICTING_JVM_DECLARATIONS!>fun <T: UserInterfaceB> testTypeParameterWithDifferentUpperBounds() {}<!>

<!CONFLICTING_JVM_DECLARATIONS!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) fun <T: Invariant<UserInterface>> testTypeParameterWithVarianceDifferentUpperBoundsA() {}<!>
<!CONFLICTING_JVM_DECLARATIONS!>fun <T: Invariant<out UserInterface>> testTypeParameterWithVarianceDifferentUpperBoundsA() {}<!>

<!CONFLICTING_JVM_DECLARATIONS!>fun <T: Invariant<UserInterface>> testTypeParameterWithVarianceDifferentUpperBoundsAReverse() {}<!>
<!CONFLICTING_JVM_DECLARATIONS!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) fun <T: Invariant<out UserInterface>> testTypeParameterWithVarianceDifferentUpperBoundsAReverse() {}<!>

<!CONFLICTING_JVM_DECLARATIONS!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) fun <T: Invariant<UserInterface>> testTypeParameterWithVarianceDifferentUpperBoundsB() {}<!>
<!CONFLICTING_JVM_DECLARATIONS!>fun <T: Invariant<in UserInterface>> testTypeParameterWithVarianceDifferentUpperBoundsB() {}<!>

<!CONFLICTING_JVM_DECLARATIONS!>fun <T: Invariant<UserInterface>> testTypeParameterWithVarianceDifferentUpperBoundsBReverse() {}<!>
<!CONFLICTING_JVM_DECLARATIONS!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) fun <T: Invariant<in UserInterface>> testTypeParameterWithVarianceDifferentUpperBoundsBReverse() {}<!>

<!CONFLICTING_JVM_DECLARATIONS!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) fun <T: Invariant<UserInterface>> testTypeParameterWithVarianceDifferentUpperBoundsC() {}<!>
<!CONFLICTING_JVM_DECLARATIONS!>fun <T: Invariant<*>> testTypeParameterWithVarianceDifferentUpperBoundsC() {}<!>

<!CONFLICTING_JVM_DECLARATIONS!>fun <T: Invariant<UserInterface>> testTypeParameterWithVarianceDifferentUpperBoundsCReverse() {}<!>
<!CONFLICTING_JVM_DECLARATIONS!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) fun <T: Invariant<*>> testTypeParameterWithVarianceDifferentUpperBoundsCReverse() {}<!>

<!CONFLICTING_JVM_DECLARATIONS!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) fun <T: Invariant<out UserInterface>> testTypeParameterWithVarianceDifferentUpperBoundsD() {}<!>
<!CONFLICTING_JVM_DECLARATIONS!>fun <T: Invariant<*>> testTypeParameterWithVarianceDifferentUpperBoundsD() {}<!>

<!CONFLICTING_JVM_DECLARATIONS!>fun <T: Invariant<out UserInterface>> testTypeParameterWithVarianceDifferentUpperBoundsDReverse() {}<!>
<!CONFLICTING_JVM_DECLARATIONS!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) fun <T: Invariant<*>> testTypeParameterWithVarianceDifferentUpperBoundsDReverse() {}<!>

<!CONFLICTING_JVM_DECLARATIONS!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) fun <T: Invariant<in UserInterface>> testTypeParameterWithVarianceDifferentUpperBoundsE() {}<!>
<!CONFLICTING_JVM_DECLARATIONS!>fun <T: Invariant<*>> testTypeParameterWithVarianceDifferentUpperBoundsE() {}<!>

<!CONFLICTING_JVM_DECLARATIONS!>fun <T: Invariant<in UserInterface>> testTypeParameterWithVarianceDifferentUpperBoundsEReverse() {}<!>
<!CONFLICTING_JVM_DECLARATIONS!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) fun <T: Invariant<*>> testTypeParameterWithVarianceDifferentUpperBoundsEReverse() {}<!>

<!CONFLICTING_JVM_DECLARATIONS!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) fun <T: Invariant<out UserInterface>> testTypeParameterWithVarianceDifferentUpperBoundsF() {}<!>
<!CONFLICTING_JVM_DECLARATIONS!>fun <T: Invariant<in UserInterface>> testTypeParameterWithVarianceDifferentUpperBoundsF() {}<!>

<!CONFLICTING_JVM_DECLARATIONS!>fun <T: Invariant<out UserInterface>> testTypeParameterWithVarianceDifferentUpperBoundsFReverse() {}<!>
<!CONFLICTING_JVM_DECLARATIONS!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) fun <T: Invariant<in UserInterface>> testTypeParameterWithVarianceDifferentUpperBoundsFReverse() {}<!>

<!CONFLICTING_JVM_DECLARATIONS!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) fun <T: UserInterface> testTypeParameterWithTypeAliasedUpperBoundsA() {}<!>
<!CONFLICTING_JVM_DECLARATIONS!>fun <T: SameUserInterface> testTypeParameterWithTypeAliasedUpperBoundsA() {}<!>

<!CONFLICTING_JVM_DECLARATIONS!>fun <T: UserInterface> testTypeParameterWithTypeAliasedUpperBoundsAReverse() {}<!>
<!CONFLICTING_JVM_DECLARATIONS!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) fun <T: SameUserInterface> testTypeParameterWithTypeAliasedUpperBoundsAReverse() {}<!>

<!CONFLICTING_JVM_DECLARATIONS!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) fun <T: UserInterface> testTypeParameterWithTypeAliasedUpperBoundsB(arg: T) {}<!>
<!CONFLICTING_JVM_DECLARATIONS!>fun <T: SameUserInterface> testTypeParameterWithTypeAliasedUpperBoundsB(arg: T) {}<!>

<!CONFLICTING_JVM_DECLARATIONS!>fun <T: UserInterface> testTypeParameterWithTypeAliasedUpperBoundsBReverse(arg: T) {}<!>
<!CONFLICTING_JVM_DECLARATIONS!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) fun <T: SameUserInterface> testTypeParameterWithTypeAliasedUpperBoundsBReverse(arg: T) {}<!>

<!CONFLICTING_JVM_DECLARATIONS!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) fun <T: UserInterface> testTypeParameterWithTypeAliasedUpperBoundsC(arg: Invariant<T>) {}<!>
<!CONFLICTING_JVM_DECLARATIONS!>fun <T: SameUserInterface> testTypeParameterWithTypeAliasedUpperBoundsC(arg: Invariant<T>) {}<!>

<!CONFLICTING_JVM_DECLARATIONS!>fun <T: UserInterface> testTypeParameterWithTypeAliasedUpperBoundsCReverse(arg: Invariant<T>) {}<!>
<!CONFLICTING_JVM_DECLARATIONS!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) fun <T: SameUserInterface> testTypeParameterWithTypeAliasedUpperBoundsCReverse(arg: Invariant<T>) {}<!>


<!CONFLICTING_JVM_DECLARATIONS!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) fun <T> testTypeParameterWithMultipleIdenticalUpperBoundsAA() where T: UserInterfaceA, T: UserInterfaceB {}<!>
<!CONFLICTING_JVM_DECLARATIONS!>fun <T> testTypeParameterWithMultipleIdenticalUpperBoundsAA() where T: UserInterfaceA, T: UserInterfaceB {}<!>

<!CONFLICTING_JVM_DECLARATIONS!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) fun <T> testTypeParameterWithMultipleIdenticalUpperBoundsAB(arg: T) where T: UserInterfaceA, T: UserInterfaceB {}<!>
<!CONFLICTING_JVM_DECLARATIONS!>fun <T> testTypeParameterWithMultipleIdenticalUpperBoundsAB(arg: T) where T: UserInterfaceA, T: UserInterfaceB {}<!>

<!CONFLICTING_JVM_DECLARATIONS!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) fun <T> testTypeParameterWithMultipleIdenticalUpperBoundsAC(arg: Invariant<T>) where T: UserInterfaceA, T: UserInterfaceB {}<!>
<!CONFLICTING_JVM_DECLARATIONS!>fun <T> testTypeParameterWithMultipleIdenticalUpperBoundsAC(arg: Invariant<T>) where T: UserInterfaceA, T: UserInterfaceB {}<!>

<!CONFLICTING_JVM_DECLARATIONS!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) fun <T: UserInterfaceA> testTypeParameterWithMultipleIdenticalUpperBoundsBA() where T: UserInterfaceB {}<!>
<!CONFLICTING_JVM_DECLARATIONS!>fun <T: UserInterfaceA> testTypeParameterWithMultipleIdenticalUpperBoundsBA() where T: UserInterfaceB {}<!>

<!CONFLICTING_JVM_DECLARATIONS!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) fun <T: UserInterfaceA> testTypeParameterWithMultipleIdenticalUpperBoundsBB(arg: T) where T: UserInterfaceB {}<!>
<!CONFLICTING_JVM_DECLARATIONS!>fun <T: UserInterfaceA> testTypeParameterWithMultipleIdenticalUpperBoundsBB(arg: T) where T: UserInterfaceB {}<!>

<!CONFLICTING_JVM_DECLARATIONS!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) fun <T: UserInterfaceA> testTypeParameterWithMultipleIdenticalUpperBoundsBC(arg: Invariant<T>) where T: UserInterfaceB {}<!>
<!CONFLICTING_JVM_DECLARATIONS!>fun <T: UserInterfaceA> testTypeParameterWithMultipleIdenticalUpperBoundsBC(arg: Invariant<T>) where T: UserInterfaceB {}<!>

<!CONFLICTING_JVM_DECLARATIONS!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) fun <T> testDifferencesInTypeParameterMultipleUpperBoundsPresenceA() where T: UserInterfaceA, T: UserInterfaceB {}<!>
<!CONFLICTING_JVM_DECLARATIONS!>fun <T> testDifferencesInTypeParameterMultipleUpperBoundsPresenceA() where T: UserInterfaceA {}<!>

<!CONFLICTING_JVM_DECLARATIONS!>fun <T: UserInterfaceA> testDifferencesInTypeParameterMultipleUpperBoundsPresenceB() where T: UserInterfaceB {}<!>
<!CONFLICTING_JVM_DECLARATIONS!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) fun <T: UserInterfaceA> testDifferencesInTypeParameterMultipleUpperBoundsPresenceB() {}<!>

<!CONFLICTING_JVM_DECLARATIONS!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) fun <T> testTypeParameterWithMultipleDifferentUpperBoundsAA() where T: UserInterfaceA, T: UserInterfaceB {}<!>
<!CONFLICTING_JVM_DECLARATIONS!>fun <T> testTypeParameterWithMultipleDifferentUpperBoundsAA() where T: UserInterfaceA, T: UserInterfaceC {}<!>

<!CONFLICTING_JVM_DECLARATIONS!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) fun <T: UserInterfaceA> testTypeParameterWithMultipleDifferentUpperBoundsAB() where T: UserInterfaceB {}<!>
<!CONFLICTING_JVM_DECLARATIONS!>fun <T: UserInterfaceA> testTypeParameterWithMultipleDifferentUpperBoundsAB() where T: UserInterfaceC {}<!>

<!CONFLICTING_JVM_DECLARATIONS!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) fun <T: UserInterfaceB> testTypeParameterWithMultipleDifferentUpperBoundsAC() where T: UserInterfaceA {}<!>
<!CONFLICTING_JVM_DECLARATIONS!>fun <T: UserInterfaceC> testTypeParameterWithMultipleDifferentUpperBoundsAC() where T: UserInterfaceA {}<!>

<!CONFLICTING_JVM_DECLARATIONS!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) fun <T> testTypeParameterWithMultipleDifferentUpperBoundsBA() where T: UserInterfaceA, T: UserInterfaceB {}<!>
<!CONFLICTING_JVM_DECLARATIONS!>fun <T> testTypeParameterWithMultipleDifferentUpperBoundsBA() where T: UserInterfaceC, T: UserInterfaceD {}<!>

<!CONFLICTING_JVM_DECLARATIONS!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) fun <T: UserInterfaceA> testTypeParameterWithMultipleDifferentUpperBoundsBB() where T: UserInterfaceB {}<!>
<!CONFLICTING_JVM_DECLARATIONS!>fun <T: UserInterfaceC> testTypeParameterWithMultipleDifferentUpperBoundsBB() where T: UserInterfaceD {}<!>

<!CONFLICTING_JVM_DECLARATIONS!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) fun <T> testTypeParameterWithMultipleTypeAliasedUpperBoundsAAA() where T: UserInterfaceA, T: UserInterfaceB {}<!>
<!CONFLICTING_JVM_DECLARATIONS!>fun <T> testTypeParameterWithMultipleTypeAliasedUpperBoundsAAA() where T: UserInterfaceA, T: SameUserInterfaceB {}<!>

<!CONFLICTING_JVM_DECLARATIONS!>fun <T> testTypeParameterWithMultipleTypeAliasedUpperBoundsAAAReverse() where T: UserInterfaceA, T: UserInterfaceB {}<!>
<!CONFLICTING_JVM_DECLARATIONS!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) fun <T> testTypeParameterWithMultipleTypeAliasedUpperBoundsAAAReverse() where T: UserInterfaceA, T: SameUserInterfaceB {}<!>

<!CONFLICTING_JVM_DECLARATIONS!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) fun <T> testTypeParameterWithMultipleTypeAliasedUpperBoundsAAB(arg: T) where T: UserInterfaceA, T: UserInterfaceB {}<!>
<!CONFLICTING_JVM_DECLARATIONS!>fun <T> testTypeParameterWithMultipleTypeAliasedUpperBoundsAAB(arg: T) where T: UserInterfaceA, T: SameUserInterfaceB {}<!>

<!CONFLICTING_JVM_DECLARATIONS!>fun <T> testTypeParameterWithMultipleTypeAliasedUpperBoundsAABReverse(arg: T) where T: UserInterfaceA, T: UserInterfaceB {}<!>
<!CONFLICTING_JVM_DECLARATIONS!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) fun <T> testTypeParameterWithMultipleTypeAliasedUpperBoundsAABReverse(arg: T) where T: UserInterfaceA, T: SameUserInterfaceB {}<!>

<!CONFLICTING_JVM_DECLARATIONS!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) fun <T> testTypeParameterWithMultipleTypeAliasedUpperBoundsAAC(arg: Invariant<T>) where T: UserInterfaceA, T: UserInterfaceB {}<!>
<!CONFLICTING_JVM_DECLARATIONS!>fun <T> testTypeParameterWithMultipleTypeAliasedUpperBoundsAAC(arg: Invariant<T>) where T: UserInterfaceA, T: SameUserInterfaceB {}<!>

<!CONFLICTING_JVM_DECLARATIONS!>fun <T> testTypeParameterWithMultipleTypeAliasedUpperBoundsAACReverse(arg: Invariant<T>) where T: UserInterfaceA, T: UserInterfaceB {}<!>
<!CONFLICTING_JVM_DECLARATIONS!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) fun <T> testTypeParameterWithMultipleTypeAliasedUpperBoundsAACReverse(arg: Invariant<T>) where T: UserInterfaceA, T: SameUserInterfaceB {}<!>

<!CONFLICTING_JVM_DECLARATIONS!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) fun <T: UserInterfaceA> testTypeParameterWithMultipleTypeAliasedUpperBoundsABA() where T: UserInterfaceB {}<!>
<!CONFLICTING_JVM_DECLARATIONS!>fun <T: UserInterfaceA> testTypeParameterWithMultipleTypeAliasedUpperBoundsABA() where T: SameUserInterfaceB {}<!>

<!CONFLICTING_JVM_DECLARATIONS!>fun <T: UserInterfaceA> testTypeParameterWithMultipleTypeAliasedUpperBoundsABAReverse() where T: UserInterfaceB {}<!>
<!CONFLICTING_JVM_DECLARATIONS!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) fun <T: UserInterfaceA> testTypeParameterWithMultipleTypeAliasedUpperBoundsABAReverse() where T: SameUserInterfaceB {}<!>

<!CONFLICTING_JVM_DECLARATIONS!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) fun <T: UserInterfaceA> testTypeParameterWithMultipleTypeAliasedUpperBoundsABB(arg: T) where T: UserInterfaceB {}<!>
<!CONFLICTING_JVM_DECLARATIONS!>fun <T: UserInterfaceA> testTypeParameterWithMultipleTypeAliasedUpperBoundsABB(arg: T) where T: SameUserInterfaceB {}<!>

<!CONFLICTING_JVM_DECLARATIONS!>fun <T: UserInterfaceA> testTypeParameterWithMultipleTypeAliasedUpperBoundsABBReverse(arg: T) where T: UserInterfaceB {}<!>
<!CONFLICTING_JVM_DECLARATIONS!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) fun <T: UserInterfaceA> testTypeParameterWithMultipleTypeAliasedUpperBoundsABBReverse(arg: T) where T: SameUserInterfaceB {}<!>

<!CONFLICTING_JVM_DECLARATIONS!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) fun <T: UserInterfaceA> testTypeParameterWithMultipleTypeAliasedUpperBoundsABC(arg: Invariant<T>) where T: UserInterfaceB {}<!>
<!CONFLICTING_JVM_DECLARATIONS!>fun <T: UserInterfaceA> testTypeParameterWithMultipleTypeAliasedUpperBoundsABC(arg: Invariant<T>) where T: SameUserInterfaceB {}<!>

<!CONFLICTING_JVM_DECLARATIONS!>fun <T: UserInterfaceA> testTypeParameterWithMultipleTypeAliasedUpperBoundsABCReverse(arg: Invariant<T>) where T: UserInterfaceB {}<!>
<!CONFLICTING_JVM_DECLARATIONS!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) fun <T: UserInterfaceA> testTypeParameterWithMultipleTypeAliasedUpperBoundsABCReverse(arg: Invariant<T>) where T: SameUserInterfaceB {}<!>

<!CONFLICTING_JVM_DECLARATIONS!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) fun <T: UserInterfaceB> testTypeParameterWithMultipleTypeAliasedUpperBoundsACA() where T: UserInterfaceA {}<!>
<!CONFLICTING_JVM_DECLARATIONS!>fun <T: SameUserInterfaceB> testTypeParameterWithMultipleTypeAliasedUpperBoundsACA() where T: UserInterfaceA {}<!>

<!CONFLICTING_JVM_DECLARATIONS!>fun <T: UserInterfaceB> testTypeParameterWithMultipleTypeAliasedUpperBoundsACAReverse() where T: UserInterfaceA {}<!>
<!CONFLICTING_JVM_DECLARATIONS!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) fun <T: SameUserInterfaceB> testTypeParameterWithMultipleTypeAliasedUpperBoundsACAReverse() where T: UserInterfaceA {}<!>

<!CONFLICTING_JVM_DECLARATIONS!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) fun <T: UserInterfaceB> testTypeParameterWithMultipleTypeAliasedUpperBoundsACB(arg: T) where T: UserInterfaceA {}<!>
<!CONFLICTING_JVM_DECLARATIONS!>fun <T: SameUserInterfaceB> testTypeParameterWithMultipleTypeAliasedUpperBoundsACB(arg: T) where T: UserInterfaceA {}<!>

<!CONFLICTING_JVM_DECLARATIONS!>fun <T: UserInterfaceB> testTypeParameterWithMultipleTypeAliasedUpperBoundsACBReverse(arg: T) where T: UserInterfaceA {}<!>
<!CONFLICTING_JVM_DECLARATIONS!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) fun <T: SameUserInterfaceB> testTypeParameterWithMultipleTypeAliasedUpperBoundsACBReverse(arg: T) where T: UserInterfaceA {}<!>

<!CONFLICTING_JVM_DECLARATIONS!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) fun <T: UserInterfaceB> testTypeParameterWithMultipleTypeAliasedUpperBoundsACC(arg: Invariant<T>) where T: UserInterfaceA {}<!>
<!CONFLICTING_JVM_DECLARATIONS!>fun <T: SameUserInterfaceB> testTypeParameterWithMultipleTypeAliasedUpperBoundsACC(arg: Invariant<T>) where T: UserInterfaceA {}<!>

<!CONFLICTING_JVM_DECLARATIONS!>fun <T: UserInterfaceB> testTypeParameterWithMultipleTypeAliasedUpperBoundsACCReverse(arg: Invariant<T>) where T: UserInterfaceA {}<!>
<!CONFLICTING_JVM_DECLARATIONS!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) fun <T: SameUserInterfaceB> testTypeParameterWithMultipleTypeAliasedUpperBoundsACCReverse(arg: Invariant<T>) where T: UserInterfaceA {}<!>

<!CONFLICTING_JVM_DECLARATIONS!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) fun <T> testTypeParameterWithMultipleTypeAliasedUpperBoundsBAA() where T: UserInterfaceA, T: UserInterfaceB {}<!>
<!CONFLICTING_JVM_DECLARATIONS!>fun <T> testTypeParameterWithMultipleTypeAliasedUpperBoundsBAA() where T: SameUserInterfaceA, T: SameUserInterfaceB {}<!>

<!CONFLICTING_JVM_DECLARATIONS!>fun <T> testTypeParameterWithMultipleTypeAliasedUpperBoundsBAAReverse() where T: UserInterfaceA, T: UserInterfaceB {}<!>
<!CONFLICTING_JVM_DECLARATIONS!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) fun <T> testTypeParameterWithMultipleTypeAliasedUpperBoundsBAAReverse() where T: SameUserInterfaceA, T: SameUserInterfaceB {}<!>

<!CONFLICTING_JVM_DECLARATIONS!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) fun <T> testTypeParameterWithMultipleTypeAliasedUpperBoundsBAB(arg: T) where T: UserInterfaceA, T: UserInterfaceB {}<!>
<!CONFLICTING_JVM_DECLARATIONS!>fun <T> testTypeParameterWithMultipleTypeAliasedUpperBoundsBAB(arg: T) where T: SameUserInterfaceA, T: SameUserInterfaceB {}<!>

<!CONFLICTING_JVM_DECLARATIONS!>fun <T> testTypeParameterWithMultipleTypeAliasedUpperBoundsBABReverse(arg: T) where T: UserInterfaceA, T: UserInterfaceB {}<!>
<!CONFLICTING_JVM_DECLARATIONS!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) fun <T> testTypeParameterWithMultipleTypeAliasedUpperBoundsBABReverse(arg: T) where T: SameUserInterfaceA, T: SameUserInterfaceB {}<!>

<!CONFLICTING_JVM_DECLARATIONS!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) fun <T> testTypeParameterWithMultipleTypeAliasedUpperBoundsBAC(arg: Invariant<T>) where T: UserInterfaceA, T: UserInterfaceB {}<!>
<!CONFLICTING_JVM_DECLARATIONS!>fun <T> testTypeParameterWithMultipleTypeAliasedUpperBoundsBAC(arg: Invariant<T>) where T: SameUserInterfaceA, T: SameUserInterfaceB {}<!>

<!CONFLICTING_JVM_DECLARATIONS!>fun <T> testTypeParameterWithMultipleTypeAliasedUpperBoundsBACReverse(arg: Invariant<T>) where T: UserInterfaceA, T: UserInterfaceB {}<!>
<!CONFLICTING_JVM_DECLARATIONS!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) fun <T> testTypeParameterWithMultipleTypeAliasedUpperBoundsBACReverse(arg: Invariant<T>) where T: SameUserInterfaceA, T: SameUserInterfaceB {}<!>

<!CONFLICTING_JVM_DECLARATIONS!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) fun <T: UserInterfaceA> testTypeParameterWithMultipleTypeAliasedUpperBoundsBBA() where T: UserInterfaceB {}<!>
<!CONFLICTING_JVM_DECLARATIONS!>fun <T: SameUserInterfaceA> testTypeParameterWithMultipleTypeAliasedUpperBoundsBBA() where T: SameUserInterfaceB {}<!>

<!CONFLICTING_JVM_DECLARATIONS!>fun <T: UserInterfaceA> testTypeParameterWithMultipleTypeAliasedUpperBoundsBBAReverse() where T: UserInterfaceB {}<!>
<!CONFLICTING_JVM_DECLARATIONS!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) fun <T: SameUserInterfaceA> testTypeParameterWithMultipleTypeAliasedUpperBoundsBBAReverse() where T: SameUserInterfaceB {}<!>

<!CONFLICTING_JVM_DECLARATIONS!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) fun <T: UserInterfaceA> testTypeParameterWithMultipleTypeAliasedUpperBoundsBBB(arg: T) where T: UserInterfaceB {}<!>
<!CONFLICTING_JVM_DECLARATIONS!>fun <T: SameUserInterfaceA> testTypeParameterWithMultipleTypeAliasedUpperBoundsBBB(arg: T) where T: SameUserInterfaceB {}<!>

<!CONFLICTING_JVM_DECLARATIONS!>fun <T: UserInterfaceA> testTypeParameterWithMultipleTypeAliasedUpperBoundsBBBReverse(arg: T) where T: UserInterfaceB {}<!>
<!CONFLICTING_JVM_DECLARATIONS!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) fun <T: SameUserInterfaceA> testTypeParameterWithMultipleTypeAliasedUpperBoundsBBBReverse(arg: T) where T: SameUserInterfaceB {}<!>

<!CONFLICTING_JVM_DECLARATIONS!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) fun <T: UserInterfaceA> testTypeParameterWithMultipleTypeAliasedUpperBoundsBBC(arg: Invariant<T>) where T: UserInterfaceB {}<!>
<!CONFLICTING_JVM_DECLARATIONS!>fun <T: SameUserInterfaceA> testTypeParameterWithMultipleTypeAliasedUpperBoundsBBC(arg: Invariant<T>) where T: SameUserInterfaceB {}<!>

<!CONFLICTING_JVM_DECLARATIONS!>fun <T: UserInterfaceA> testTypeParameterWithMultipleTypeAliasedUpperBoundsBBCReverse(arg: Invariant<T>) where T: UserInterfaceB {}<!>
<!CONFLICTING_JVM_DECLARATIONS!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) fun <T: SameUserInterfaceA> testTypeParameterWithMultipleTypeAliasedUpperBoundsBBCReverse(arg: Invariant<T>) where T: SameUserInterfaceB {}<!>

<!CONFLICTING_JVM_DECLARATIONS!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) fun <T> testTypeParameterWithMultipleShuffledUpperBoundsAA() where T: UserInterfaceA, T: UserInterfaceB {}<!>
<!CONFLICTING_JVM_DECLARATIONS!>fun <T> testTypeParameterWithMultipleShuffledUpperBoundsAA() where T: UserInterfaceB, T: UserInterfaceA {}<!>

@Deprecated(message = "", level = DeprecationLevel.HIDDEN) fun <T> testTypeParameterWithMultipleShuffledUpperBoundsAB(arg: T) where T: UserInterfaceA, T: UserInterfaceB {}
fun <T> testTypeParameterWithMultipleShuffledUpperBoundsAB(arg: T) where T: UserInterfaceB, T: UserInterfaceA {}

<!CONFLICTING_JVM_DECLARATIONS!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) fun <T> testTypeParameterWithMultipleShuffledUpperBoundsAC(arg: Invariant<T>) where T: UserInterfaceA, T: UserInterfaceB {}<!>
<!CONFLICTING_JVM_DECLARATIONS!>fun <T> testTypeParameterWithMultipleShuffledUpperBoundsAC(arg: Invariant<T>) where T: UserInterfaceB, T: UserInterfaceA {}<!>

<!CONFLICTING_JVM_DECLARATIONS!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) fun <T: UserInterfaceA> testTypeParameterWithMultipleShuffledUpperBoundsBA() where T: UserInterfaceB {}<!>
<!CONFLICTING_JVM_DECLARATIONS!>fun <T: UserInterfaceB> testTypeParameterWithMultipleShuffledUpperBoundsBA() where T: UserInterfaceA {}<!>

@Deprecated(message = "", level = DeprecationLevel.HIDDEN) fun <T: UserInterfaceA> testTypeParameterWithMultipleShuffledUpperBoundsBB(arg: T) where T: UserInterfaceB {}
fun <T: UserInterfaceB> testTypeParameterWithMultipleShuffledUpperBoundsBB(arg: T) where T: UserInterfaceA {}

<!CONFLICTING_JVM_DECLARATIONS!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) fun <T: UserInterfaceA> testTypeParameterWithMultipleShuffledUpperBoundsBC(arg: Invariant<T>) where T: UserInterfaceB {}<!>
<!CONFLICTING_JVM_DECLARATIONS!>fun <T: UserInterfaceB> testTypeParameterWithMultipleShuffledUpperBoundsBC(arg: Invariant<T>) where T: UserInterfaceA {}<!>


<!CONFLICTING_JVM_DECLARATIONS!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) inline fun <reified T> testIdenticalReifiedTypeParameterA() {}<!>
<!CONFLICTING_JVM_DECLARATIONS!>inline fun <reified T> testIdenticalReifiedTypeParameterA() {}<!>

<!CONFLICTING_JVM_DECLARATIONS!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) inline fun <reified T> testIdenticalReifiedTypeParameterB(arg: T) {}<!>
<!CONFLICTING_JVM_DECLARATIONS!>inline fun <reified T> testIdenticalReifiedTypeParameterB(arg: T) {}<!>

<!CONFLICTING_JVM_DECLARATIONS!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) inline fun <reified T> testIdenticalReifiedTypeParameterC(arg: Invariant<T>) {}<!>
<!CONFLICTING_JVM_DECLARATIONS!>inline fun <reified T> testIdenticalReifiedTypeParameterC(arg: Invariant<T>) {}<!>

<!CONFLICTING_JVM_DECLARATIONS!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) inline fun <reified T> testDifferencesInReifiedBehaviorOfTypeParameterA() {}<!>
<!CONFLICTING_JVM_DECLARATIONS!>inline fun <T> testDifferencesInReifiedBehaviorOfTypeParameterA() {}<!>

<!CONFLICTING_JVM_DECLARATIONS!>inline fun <reified T> testDifferencesInReifiedBehaviorOfTypeParameterAReverse() {}<!>
<!CONFLICTING_JVM_DECLARATIONS!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) inline fun <T> testDifferencesInReifiedBehaviorOfTypeParameterAReverse() {}<!>

<!CONFLICTING_JVM_DECLARATIONS!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) inline fun <reified T> testDifferencesInReifiedBehaviorOfTypeParameterB(arg: T) {}<!>
<!CONFLICTING_JVM_DECLARATIONS!>inline fun <T> testDifferencesInReifiedBehaviorOfTypeParameterB(arg: T) {}<!>

<!CONFLICTING_JVM_DECLARATIONS!>inline fun <reified T> testDifferencesInReifiedBehaviorOfTypeParameterBReverse(arg: T) {}<!>
<!CONFLICTING_JVM_DECLARATIONS!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) inline fun <T> testDifferencesInReifiedBehaviorOfTypeParameterBReverse(arg: T) {}<!>

<!CONFLICTING_JVM_DECLARATIONS!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) inline fun <reified T> testDifferencesInReifiedBehaviorOfTypeParameterC(arg: Invariant<T>) {}<!>
<!CONFLICTING_JVM_DECLARATIONS!>inline fun <T> testDifferencesInReifiedBehaviorOfTypeParameterC(arg: Invariant<T>) {}<!>

<!CONFLICTING_JVM_DECLARATIONS!>inline fun <reified T> testDifferencesInReifiedBehaviorOfTypeParameterCReverse(arg: Invariant<T>) {}<!>
<!CONFLICTING_JVM_DECLARATIONS!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) inline fun <T> testDifferencesInReifiedBehaviorOfTypeParameterCReverse(arg: Invariant<T>) {}<!>


<!CONFLICTING_JVM_DECLARATIONS!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) inline fun testIdenticalPresenceOfInlineModifier() {}<!>
<!CONFLICTING_JVM_DECLARATIONS!>inline fun testIdenticalPresenceOfInlineModifier() {}<!>

<!CONFLICTING_JVM_DECLARATIONS!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) inline fun testDifferencesInInlineModifierPresence() {}<!>
<!CONFLICTING_JVM_DECLARATIONS!>fun testDifferencesInInlineModifierPresence() {}<!>

<!CONFLICTING_JVM_DECLARATIONS!>inline fun testDifferencesInInlineModifierPresenceReverse() {}<!>
<!CONFLICTING_JVM_DECLARATIONS!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) fun testDifferencesInInlineModifierPresenceReverse() {}<!>


<!CONFLICTING_JVM_DECLARATIONS!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) infix fun UserKlass.testIdenticalPresenceOfInfixModifier(arg: UserKlass) {}<!>
<!CONFLICTING_JVM_DECLARATIONS!>infix fun UserKlass.testIdenticalPresenceOfInfixModifier(arg: UserKlass) {}<!>

<!CONFLICTING_JVM_DECLARATIONS!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) infix fun UserKlass.testDifferencesInInfixModifierPresence(arg: UserKlass) {}<!>
<!CONFLICTING_JVM_DECLARATIONS!>fun UserKlass.testDifferencesInInfixModifierPresence(arg: UserKlass) {}<!>

<!CONFLICTING_JVM_DECLARATIONS!>infix fun UserKlass.testDifferencesInInfixModifierPresenceReverse(arg: UserKlass) {}<!>
<!CONFLICTING_JVM_DECLARATIONS!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) fun UserKlass.testDifferencesInInfixModifierPresenceReverse(arg: UserKlass) {}<!>


<!CONFLICTING_JVM_DECLARATIONS!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) operator fun UserKlassA.unaryPlus() {}<!>
<!CONFLICTING_JVM_DECLARATIONS!>operator fun UserKlassA.unaryPlus() {}<!>

<!CONFLICTING_JVM_DECLARATIONS!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) operator fun UserKlassB.unaryPlus() {}<!>
<!CONFLICTING_JVM_DECLARATIONS!>fun UserKlassB.unaryPlus() {}<!>

<!CONFLICTING_JVM_DECLARATIONS!>operator fun UserKlassB.unaryMinus() {}<!>
<!CONFLICTING_JVM_DECLARATIONS!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) fun UserKlassB.unaryMinus() {}<!>


<!CONFLICTING_JVM_DECLARATIONS!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) tailrec fun testIdenticalPresenceOfTailrecModifier() {}<!>
<!CONFLICTING_JVM_DECLARATIONS!>tailrec fun testIdenticalPresenceOfTailrecModifier() {}<!>

<!CONFLICTING_JVM_DECLARATIONS!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) tailrec fun testDifferencesInTailrecModifierPresence() {}<!>
<!CONFLICTING_JVM_DECLARATIONS!>fun testDifferencesInTailrecModifierPresence() {}<!>

<!CONFLICTING_JVM_DECLARATIONS!>tailrec fun testDifferencesInTailrecModifierPresenceReverse() {}<!>
<!CONFLICTING_JVM_DECLARATIONS!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) fun testDifferencesInTailrecModifierPresenceReverse() {}<!>


<!CONFLICTING_JVM_DECLARATIONS!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) private fun testIdenticalPrivateVisibility() {}<!>
<!CONFLICTING_JVM_DECLARATIONS!>private fun testIdenticalPrivateVisibility() {}<!>

<!CONFLICTING_JVM_DECLARATIONS!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) internal fun testIdenticalInternalVisibility() {}<!>
<!CONFLICTING_JVM_DECLARATIONS!>internal fun testIdenticalInternalVisibility() {}<!>

<!CONFLICTING_JVM_DECLARATIONS!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) private fun testDifferencesInPrivateAndPublicVisibilities() {}<!>
<!CONFLICTING_JVM_DECLARATIONS!>public fun testDifferencesInPrivateAndPublicVisibilities() {}<!>

<!CONFLICTING_JVM_DECLARATIONS!>private fun testDifferencesInPrivateAndPublicVisibilitiesReverse() {}<!>
<!CONFLICTING_JVM_DECLARATIONS!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) public fun testDifferencesInPrivateAndPublicVisibilitiesReverse() {}<!>

<!CONFLICTING_JVM_DECLARATIONS!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) internal fun testDifferencesInInternalAndPublicVisibilities() {}<!>
<!CONFLICTING_JVM_DECLARATIONS!>public fun testDifferencesInInternalAndPublicVisibilities() {}<!>

<!CONFLICTING_JVM_DECLARATIONS!>internal fun testDifferencesInInternalAndPublicVisibilitiesReverse() {}<!>
<!CONFLICTING_JVM_DECLARATIONS!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) public fun testDifferencesInInternalAndPublicVisibilitiesReverse() {}<!>

<!CONFLICTING_JVM_DECLARATIONS!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) private fun testDifferencesInPrivateAndInternalVisibilities() {}<!>
<!CONFLICTING_JVM_DECLARATIONS!>internal fun testDifferencesInPrivateAndInternalVisibilities() {}<!>

<!CONFLICTING_JVM_DECLARATIONS!>private fun testDifferencesInPrivateAndInternalVisibilitiesReverse() {}<!>
<!CONFLICTING_JVM_DECLARATIONS!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) internal fun testDifferencesInPrivateAndInternalVisibilitiesReverse() {}<!>


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
