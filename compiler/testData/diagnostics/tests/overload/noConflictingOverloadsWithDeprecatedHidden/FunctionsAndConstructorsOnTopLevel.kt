// DIAGNOSTICS: -NOTHING_TO_INLINE, -NO_TAIL_CALLS_FOUND, -MISPLACED_TYPE_PARAMETER_CONSTRAINTS


class TestBasic {
    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) <!CONFLICTING_OVERLOADS!>constructor()<!>
}
<!CONFLICTING_OVERLOADS!>fun TestBasic()<!> {}

class TestBasicReverse {
    <!CONFLICTING_OVERLOADS!>constructor()<!>
}
<!CONFLICTING_OVERLOADS!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) fun TestBasicReverse()<!> {}


class TestIdenticalReturnTypes {
    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) <!CONFLICTING_OVERLOADS!>constructor()<!>
}
<!CONFLICTING_OVERLOADS!>fun TestIdenticalReturnTypes(): TestIdenticalReturnTypes<!> = TestIdenticalReturnTypes()

class TestIdenticalReturnTypesReverse {
    <!CONFLICTING_OVERLOADS!>constructor()<!>
}
<!CONFLICTING_OVERLOADS!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) fun TestIdenticalReturnTypesReverse(): TestIdenticalReturnTypesReverse<!> = TestIdenticalReturnTypesReverse()


class TestFunctionWithReifiedTypeParameterVsConstructorA<T> {
    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) <!CONFLICTING_OVERLOADS!>constructor()<!>
}
<!CONFLICTING_OVERLOADS!>inline fun <reified T> TestFunctionWithReifiedTypeParameterVsConstructorA()<!> {}

class TestFunctionWithReifiedTypeParameterVsConstructorAReverse<T> {
    <!CONFLICTING_OVERLOADS!>constructor()<!>
}
<!CONFLICTING_OVERLOADS!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) inline fun <reified T> TestFunctionWithReifiedTypeParameterVsConstructorAReverse()<!> {}

class TestFunctionWithReifiedTypeParameterVsConstructorB<T> {
    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) <!CONFLICTING_OVERLOADS!>constructor(arg: T)<!>
}
<!CONFLICTING_OVERLOADS!>inline fun <reified T> TestFunctionWithReifiedTypeParameterVsConstructorB(arg: T)<!> {}

class TestFunctionWithReifiedTypeParameterVsConstructorBReverse<T> {
    <!CONFLICTING_OVERLOADS!>constructor(arg: T)<!>
}
<!CONFLICTING_OVERLOADS!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) inline fun <reified T> TestFunctionWithReifiedTypeParameterVsConstructorBReverse(arg: T)<!> {}

class TestFunctionWithReifiedTypeParameterVsConstructorC<T> {
    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) <!CONFLICTING_OVERLOADS!>constructor(arg: Invariant<T>)<!>
}
<!CONFLICTING_OVERLOADS!>inline fun <reified T> TestFunctionWithReifiedTypeParameterVsConstructorC(arg: Invariant<T>)<!> {}

class TestFunctionWithReifiedTypeParameterVsConstructorCReverse<T> {
    <!CONFLICTING_OVERLOADS!>constructor(arg: Invariant<T>)<!>
}
<!CONFLICTING_OVERLOADS!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) inline fun <reified T> TestFunctionWithReifiedTypeParameterVsConstructorCReverse(arg: Invariant<T>)<!> {}


class TestInlineFunctionVsConstructor {
    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) <!CONFLICTING_OVERLOADS!>constructor()<!>
}
<!CONFLICTING_OVERLOADS!>inline fun TestInlineFunctionVsConstructor()<!> {}

class TestInlineFunctionVsConstructorReverse {
    <!CONFLICTING_OVERLOADS!>constructor()<!>
}
<!CONFLICTING_OVERLOADS!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) inline fun TestInlineFunctionVsConstructorReverse()<!> {}


class TestTailrecFunctionVsConstructor {
    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) <!CONFLICTING_OVERLOADS!>constructor()<!>
}
<!CONFLICTING_OVERLOADS!>tailrec fun TestTailrecFunctionVsConstructor()<!> {}

class TestTailrecFunctionVsConstructorReverse {
    <!CONFLICTING_OVERLOADS!>constructor()<!>
}
<!CONFLICTING_OVERLOADS!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) tailrec fun TestTailrecFunctionVsConstructorReverse()<!> {}


class TestFunctionVsPrimaryConstructor @Deprecated(message = "", level = DeprecationLevel.HIDDEN) <!CONFLICTING_OVERLOADS!>constructor()<!>
<!CONFLICTING_OVERLOADS!>fun TestFunctionVsPrimaryConstructor()<!> {}

class TestFunctionVsPrimaryConstructorReverse <!CONFLICTING_OVERLOADS!>constructor()<!> {}
<!CONFLICTING_OVERLOADS!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) fun TestFunctionVsPrimaryConstructorReverse()<!> {}


class TestFunctionVsDelegatedPrimaryConstructorCall constructor(placeholder: UserKlass) {
    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) <!CONFLICTING_OVERLOADS!>constructor()<!> : this(UserKlass())
}
<!CONFLICTING_OVERLOADS!>fun TestFunctionVsDelegatedPrimaryConstructorCall()<!> {}

class TestFunctionVsDelegatedPrimaryConstructorCallReverse constructor(placeholder: UserKlass) {
    <!CONFLICTING_OVERLOADS!>constructor()<!> : this(UserKlass())
}
<!CONFLICTING_OVERLOADS!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) fun TestFunctionVsDelegatedPrimaryConstructorCallReverse()<!> {}


open class SuperConstructorSource constructor(placeholder: UserKlass)

class TestFunctionVsDelegatedSuperConstructorCall: SuperConstructorSource {
    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) <!CONFLICTING_OVERLOADS!>constructor()<!> : super(UserKlass())
}
<!CONFLICTING_OVERLOADS!>fun TestFunctionVsDelegatedSuperConstructorCall()<!> {}

class TestFunctionVsDelegatedSuperConstructorCallReverse: SuperConstructorSource {
    <!CONFLICTING_OVERLOADS!>constructor()<!> : super(UserKlass())
}
<!CONFLICTING_OVERLOADS!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) fun TestFunctionVsDelegatedSuperConstructorCallReverse()<!> {}


class TestIdenticalValueParameters {
    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) <!CONFLICTING_OVERLOADS!>constructor(arg: UserKlass)<!>
}
<!CONFLICTING_OVERLOADS!>fun TestIdenticalValueParameters(arg: UserKlass)<!> {}

class TestIdenticalValueParametersReverse {
    <!CONFLICTING_OVERLOADS!>constructor(arg: UserKlass)<!>
}
<!CONFLICTING_OVERLOADS!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) fun TestIdenticalValueParametersReverse(arg: UserKlass)<!> {}

class TestDifferentlyNamedValueParameters {
    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) <!CONFLICTING_OVERLOADS!>constructor(argA: UserKlass)<!>
}
<!CONFLICTING_OVERLOADS!>fun TestDifferentlyNamedValueParameters(argB: UserKlass)<!> {}

class TestDifferentlyNamedValueParametersReverse {
    <!CONFLICTING_OVERLOADS!>constructor(argA: UserKlass)<!>
}
<!CONFLICTING_OVERLOADS!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) fun TestDifferentlyNamedValueParametersReverse(argB: UserKlass)<!> {}

class TestTypeAliasedValueParameterTypesA {
    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) <!CONFLICTING_OVERLOADS!>constructor(arg: UserKlass)<!>
}
<!CONFLICTING_OVERLOADS!>fun TestTypeAliasedValueParameterTypesA(arg: SameUserKlass)<!> {}

class TestTypeAliasedValueParameterTypesAReverse {
    <!CONFLICTING_OVERLOADS!>constructor(arg: UserKlass)<!>
}
<!CONFLICTING_OVERLOADS!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) fun TestTypeAliasedValueParameterTypesAReverse(arg: SameUserKlass)<!> {}

class TestTypeAliasedValueParameterTypesB {
    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) <!CONFLICTING_OVERLOADS!>constructor(arg: SameUserKlass)<!>
}
<!CONFLICTING_OVERLOADS!>fun TestTypeAliasedValueParameterTypesB(arg: UserKlass)<!> {}

class TestTypeAliasedValueParameterTypesBReverse {
    <!CONFLICTING_OVERLOADS!>constructor(arg: SameUserKlass)<!>
}
<!CONFLICTING_OVERLOADS!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) fun TestTypeAliasedValueParameterTypesBReverse(arg: UserKlass)<!> {}


class TestMultipleIdenticalValueParameters {
    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) <!CONFLICTING_OVERLOADS!>constructor(arg1: UserKlassA, arg2: UserKlassB)<!>
}
<!CONFLICTING_OVERLOADS!>fun TestMultipleIdenticalValueParameters(arg1: UserKlassA, arg2: UserKlassB)<!> {}

class TestMultipleIdenticalValueParametersReverse {
    <!CONFLICTING_OVERLOADS!>constructor(arg1: UserKlassA, arg2: UserKlassB)<!>
}
<!CONFLICTING_OVERLOADS!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) fun TestMultipleIdenticalValueParametersReverse(arg1: UserKlassA, arg2: UserKlassB)<!> {}

class TestMultipleDifferentlyNamedValueParametersA {
    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) <!CONFLICTING_OVERLOADS!>constructor(arg1: UserKlassA, arg2A: UserKlassB)<!>
}
<!CONFLICTING_OVERLOADS!>fun TestMultipleDifferentlyNamedValueParametersA(arg1: UserKlassA, arg2B: UserKlassB)<!> {}

class TestMultipleDifferentlyNamedValueParametersAReverse {
    <!CONFLICTING_OVERLOADS!>constructor(arg1: UserKlassA, arg2A: UserKlassB)<!>
}
<!CONFLICTING_OVERLOADS!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) fun TestMultipleDifferentlyNamedValueParametersAReverse(arg1: UserKlassA, arg2B: UserKlassB)<!> {}

class TestMultipleDifferentlyNamedValueParametersB {
    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) <!CONFLICTING_OVERLOADS!>constructor(arg1A: UserKlassA, arg2A: UserKlassB)<!>
}
<!CONFLICTING_OVERLOADS!>fun TestMultipleDifferentlyNamedValueParametersB(arg1B: UserKlassA, arg2B: UserKlassB)<!> {}

class TestMultipleDifferentlyNamedValueParametersBReverse {
    <!CONFLICTING_OVERLOADS!>constructor(arg1A: UserKlassA, arg2A: UserKlassB)<!>
}
<!CONFLICTING_OVERLOADS!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) fun TestMultipleDifferentlyNamedValueParametersBReverse(arg1B: UserKlassA, arg2B: UserKlassB)<!> {}

class TestMultipleTypeAliasedValueParameterTypesA {
    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) <!CONFLICTING_OVERLOADS!>constructor(arg1: UserKlassA, arg2: SameUserKlassB)<!>
}
<!CONFLICTING_OVERLOADS!>fun TestMultipleTypeAliasedValueParameterTypesA(arg1: UserKlassA, arg2: SameUserKlassB)<!> {}

class TestMultipleTypeAliasedValueParameterTypesAReverse {
    <!CONFLICTING_OVERLOADS!>constructor(arg1: UserKlassA, arg2: SameUserKlassB)<!>
}
<!CONFLICTING_OVERLOADS!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) fun TestMultipleTypeAliasedValueParameterTypesAReverse(arg1: UserKlassA, arg2: SameUserKlassB)<!> {}

class TestMultipleTypeAliasedValueParameterTypesB {
    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) <!CONFLICTING_OVERLOADS!>constructor(arg1: SameUserKlassA, arg2: SameUserKlassB)<!>
}
<!CONFLICTING_OVERLOADS!>fun TestMultipleTypeAliasedValueParameterTypesB(arg1: SameUserKlassA, arg2: SameUserKlassB)<!> {}

class TestMultipleTypeAliasedValueParameterTypesBReverse {
    <!CONFLICTING_OVERLOADS!>constructor(arg1: SameUserKlassA, arg2: SameUserKlassB)<!>
}
<!CONFLICTING_OVERLOADS!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) fun TestMultipleTypeAliasedValueParameterTypesBReverse(arg1: SameUserKlassA, arg2: SameUserKlassB)<!> {}


class TestIdenticalTypeParametersA<T> {
    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) <!CONFLICTING_OVERLOADS!>constructor()<!>
}
<!CONFLICTING_OVERLOADS!>fun <T> TestIdenticalTypeParametersA()<!> {}

class TestIdenticalTypeParametersAReverse<T> {
    <!CONFLICTING_OVERLOADS!>constructor()<!>
}
<!CONFLICTING_OVERLOADS!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) fun <T> TestIdenticalTypeParametersAReverse()<!> {}

class TestIdenticalTypeParametersB<T> {
    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) <!CONFLICTING_OVERLOADS!>constructor(arg: T)<!>
}
<!CONFLICTING_OVERLOADS!>fun <T> TestIdenticalTypeParametersB(arg: T)<!> {}

class TestIdenticalTypeParametersBReverse<T> {
    <!CONFLICTING_OVERLOADS!>constructor(arg: T)<!>
}
<!CONFLICTING_OVERLOADS!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) fun <T> TestIdenticalTypeParametersBReverse(arg: T)<!> {}

class TestIdenticalTypeParametersC<T> {
    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) <!CONFLICTING_OVERLOADS!>constructor(arg: Invariant<T>)<!>
}
<!CONFLICTING_OVERLOADS!>fun <T> TestIdenticalTypeParametersC(arg: Invariant<T>)<!> {}

class TestIdenticalTypeParametersCReverse<T> {
    <!CONFLICTING_OVERLOADS!>constructor(arg: Invariant<T>)<!>
}
<!CONFLICTING_OVERLOADS!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) fun <T> TestIdenticalTypeParametersCReverse(arg: Invariant<T>)<!> {}


class TestMultipleIdenticalTypeParameters<T1, T2> {
    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) <!CONFLICTING_OVERLOADS!>constructor()<!>
}
<!CONFLICTING_OVERLOADS!>fun <T1, T2> TestMultipleIdenticalTypeParameters()<!> {}

class TestMultipleIdenticalTypeParametersReverse<T1, T2> {
    <!CONFLICTING_OVERLOADS!>constructor()<!>
}
<!CONFLICTING_OVERLOADS!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) fun <T1, T2> TestMultipleIdenticalTypeParametersReverse()<!> {}


class TestTypeParameterWithIdenticalUpperBoundsA<T: UserInterface> {
    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) <!CONFLICTING_OVERLOADS!>constructor()<!>
}
<!CONFLICTING_OVERLOADS!>fun <T: UserInterface> TestTypeParameterWithIdenticalUpperBoundsA()<!> {}

class TestTypeParameterWithIdenticalUpperBoundsAReverse<T: UserInterface> {
    <!CONFLICTING_OVERLOADS!>constructor()<!>
}
<!CONFLICTING_OVERLOADS!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) fun <T: UserInterface> TestTypeParameterWithIdenticalUpperBoundsAReverse()<!> {}

class TestTypeParameterWithIdenticalUpperBoundsB<T: UserInterface> {
    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) <!CONFLICTING_OVERLOADS!>constructor(arg: T)<!>
}
<!CONFLICTING_OVERLOADS!>fun <T: UserInterface> TestTypeParameterWithIdenticalUpperBoundsB(arg: T)<!> {}

class TestTypeParameterWithIdenticalUpperBoundsBReverse<T: UserInterface> {
    <!CONFLICTING_OVERLOADS!>constructor(arg: T)<!>
}
<!CONFLICTING_OVERLOADS!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) fun <T: UserInterface> TestTypeParameterWithIdenticalUpperBoundsBReverse(arg: T)<!> {}

class TestTypeParameterWithIdenticalUpperBoundsC<T: UserInterface> {
    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) <!CONFLICTING_OVERLOADS!>constructor(arg: Invariant<T>)<!>
}
<!CONFLICTING_OVERLOADS!>fun <T: UserInterface> TestTypeParameterWithIdenticalUpperBoundsC(arg: Invariant<T>)<!> {}

class TestTypeParameterWithIdenticalUpperBoundsCReverse<T: UserInterface> {
    <!CONFLICTING_OVERLOADS!>constructor(arg: Invariant<T>)<!>
}
<!CONFLICTING_OVERLOADS!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) fun <T: UserInterface> TestTypeParameterWithIdenticalUpperBoundsCReverse(arg: Invariant<T>)<!> {}


class TestTypeParameterWithMultipleIdenticalUpperBoundsAA<T> where T: UserInterfaceA, T: UserInterfaceB {
    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) <!CONFLICTING_OVERLOADS!>constructor()<!>
}
<!CONFLICTING_OVERLOADS!>fun <T> TestTypeParameterWithMultipleIdenticalUpperBoundsAA()<!> where T: UserInterfaceA, T: UserInterfaceB {}

class TestTypeParameterWithMultipleIdenticalUpperBoundsAAReverse<T> where T: UserInterfaceA, T: UserInterfaceB {
    <!CONFLICTING_OVERLOADS!>constructor()<!>
}
<!CONFLICTING_OVERLOADS!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) fun <T> TestTypeParameterWithMultipleIdenticalUpperBoundsAAReverse()<!> where T: UserInterfaceA, T: UserInterfaceB {}

class TestTypeParameterWithMultipleIdenticalUpperBoundsAB<T> where T: UserInterfaceA, T: UserInterfaceB {
    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) <!CONFLICTING_OVERLOADS!>constructor(arg: T)<!>
}
<!CONFLICTING_OVERLOADS!>fun <T> TestTypeParameterWithMultipleIdenticalUpperBoundsAB(arg: T)<!> where T: UserInterfaceA, T: UserInterfaceB {}

class TestTypeParameterWithMultipleIdenticalUpperBoundsABReverse<T> where T: UserInterfaceA, T: UserInterfaceB {
    <!CONFLICTING_OVERLOADS!>constructor(arg: T)<!>
}
<!CONFLICTING_OVERLOADS!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) fun <T> TestTypeParameterWithMultipleIdenticalUpperBoundsABReverse(arg: T)<!> where T: UserInterfaceA, T: UserInterfaceB {}

class TestTypeParameterWithMultipleIdenticalUpperBoundsAC<T> where T: UserInterfaceA, T: UserInterfaceB {
    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) <!CONFLICTING_OVERLOADS!>constructor(arg: Invariant<T>)<!>
}
<!CONFLICTING_OVERLOADS!>fun <T> TestTypeParameterWithMultipleIdenticalUpperBoundsAC(arg: Invariant<T>)<!> where T: UserInterfaceA, T: UserInterfaceB {}

class TestTypeParameterWithMultipleIdenticalUpperBoundsACReverse<T> where T: UserInterfaceA, T: UserInterfaceB {
    <!CONFLICTING_OVERLOADS!>constructor(arg: Invariant<T>)<!>
}
<!CONFLICTING_OVERLOADS!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) fun <T> TestTypeParameterWithMultipleIdenticalUpperBoundsACReverse(arg: Invariant<T>)<!> where T: UserInterfaceA, T: UserInterfaceB {}

class TestTypeParameterWithMultipleIdenticalUpperBoundsBA<T: UserInterfaceA> where T: UserInterfaceB {
    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) <!CONFLICTING_OVERLOADS!>constructor()<!>
}
<!CONFLICTING_OVERLOADS!>fun <T: UserInterfaceA> TestTypeParameterWithMultipleIdenticalUpperBoundsBA()<!> where T: UserInterfaceB {}

class TestTypeParameterWithMultipleIdenticalUpperBoundsBAReverse<T: UserInterfaceA> where T: UserInterfaceB {
    <!CONFLICTING_OVERLOADS!>constructor()<!>
}
<!CONFLICTING_OVERLOADS!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) fun <T: UserInterfaceA> TestTypeParameterWithMultipleIdenticalUpperBoundsBAReverse()<!> where T: UserInterfaceB {}

class TestTypeParameterWithMultipleIdenticalUpperBoundsBB<T: UserInterfaceA> where T: UserInterfaceB {
    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) <!CONFLICTING_OVERLOADS!>constructor(arg: T)<!>
}
<!CONFLICTING_OVERLOADS!>fun <T: UserInterfaceA> TestTypeParameterWithMultipleIdenticalUpperBoundsBB(arg: T)<!> where T: UserInterfaceB {}

class TestTypeParameterWithMultipleIdenticalUpperBoundsBBReverse<T: UserInterfaceA> where T: UserInterfaceB {
    <!CONFLICTING_OVERLOADS!>constructor(arg: T)<!>
}
<!CONFLICTING_OVERLOADS!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) fun <T: UserInterfaceA> TestTypeParameterWithMultipleIdenticalUpperBoundsBBReverse(arg: T)<!> where T: UserInterfaceB {}

class TestTypeParameterWithMultipleIdenticalUpperBoundsBC<T: UserInterfaceA> where T: UserInterfaceB {
    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) <!CONFLICTING_OVERLOADS!>constructor(arg: Invariant<T>)<!>
}
<!CONFLICTING_OVERLOADS!>fun <T: UserInterfaceA> TestTypeParameterWithMultipleIdenticalUpperBoundsBC(arg: Invariant<T>)<!> where T: UserInterfaceB {}

class TestTypeParameterWithMultipleIdenticalUpperBoundsBCReverse<T: UserInterfaceA> where T: UserInterfaceB {
    <!CONFLICTING_OVERLOADS!>constructor(arg: Invariant<T>)<!>
}
<!CONFLICTING_OVERLOADS!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) fun <T: UserInterfaceA> TestTypeParameterWithMultipleIdenticalUpperBoundsBCReverse(arg: Invariant<T>)<!> where T: UserInterfaceB {}


class TestIdenticalPrivateVisibility {
    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) private <!CONFLICTING_OVERLOADS!>constructor()<!>
}
<!CONFLICTING_OVERLOADS!>private fun TestIdenticalPrivateVisibility()<!> {}

class TestIdenticalPrivateVisibilityReverse {
    private <!CONFLICTING_OVERLOADS!>constructor()<!>
}
<!CONFLICTING_OVERLOADS!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) private fun TestIdenticalPrivateVisibilityReverse()<!> {}

class TestIdenticalInternalVisibility {
    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) internal <!CONFLICTING_OVERLOADS!>constructor()<!>
}
<!CONFLICTING_OVERLOADS!>internal fun TestIdenticalInternalVisibility()<!> {}

class TestIdenticalInternalVisibilityReverse {
    internal <!CONFLICTING_OVERLOADS!>constructor()<!>
}
<!CONFLICTING_OVERLOADS!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) internal fun TestIdenticalInternalVisibilityReverse()<!> {}

class TestDifferencesInPrivateAndPublicVisibilitiesA {
    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) private <!CONFLICTING_OVERLOADS!>constructor()<!>
}
<!CONFLICTING_OVERLOADS!>public fun TestDifferencesInPrivateAndPublicVisibilitiesA()<!> {}

class TestDifferencesInPrivateAndPublicVisibilitiesAReverse {
    private <!CONFLICTING_OVERLOADS!>constructor()<!>
}
<!CONFLICTING_OVERLOADS!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) public fun TestDifferencesInPrivateAndPublicVisibilitiesAReverse()<!> {}

class TestDifferencesInPrivateAndPublicVisibilitiesB {
    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) public <!CONFLICTING_OVERLOADS!>constructor()<!>
}
<!CONFLICTING_OVERLOADS!>private fun TestDifferencesInPrivateAndPublicVisibilitiesB()<!> {}

class TestDifferencesInPrivateAndPublicVisibilitiesBReverse {
    public <!CONFLICTING_OVERLOADS!>constructor()<!>
}
<!CONFLICTING_OVERLOADS!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) private fun TestDifferencesInPrivateAndPublicVisibilitiesBReverse()<!> {}

class TestDifferencesInInternalAndPublicVisibilitiesA {
    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) internal <!CONFLICTING_OVERLOADS!>constructor()<!>
}
<!CONFLICTING_OVERLOADS!>public fun TestDifferencesInInternalAndPublicVisibilitiesA()<!> {}

class TestDifferencesInInternalAndPublicVisibilitiesAReverse {
    internal <!CONFLICTING_OVERLOADS!>constructor()<!>
}
<!CONFLICTING_OVERLOADS!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) public fun TestDifferencesInInternalAndPublicVisibilitiesAReverse()<!> {}

class TestDifferencesInInternalAndPublicVisibilitiesB {
    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) public <!CONFLICTING_OVERLOADS!>constructor()<!>
}
<!CONFLICTING_OVERLOADS!>internal fun TestDifferencesInInternalAndPublicVisibilitiesB()<!> {}

class TestDifferencesInInternalAndPublicVisibilitiesBReverse {
    public <!CONFLICTING_OVERLOADS!>constructor()<!>
}
<!CONFLICTING_OVERLOADS!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) internal fun TestDifferencesInInternalAndPublicVisibilitiesBReverse()<!> {}

class TestDifferencesInPrivateAndInternalVisibilitiesA {
    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) private <!CONFLICTING_OVERLOADS!>constructor()<!>
}
<!CONFLICTING_OVERLOADS!>internal fun TestDifferencesInPrivateAndInternalVisibilitiesA()<!> {}

class TestDifferencesInPrivateAndInternalVisibilitiesAReverse {
    private <!CONFLICTING_OVERLOADS!>constructor()<!>
}
<!CONFLICTING_OVERLOADS!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) internal fun TestDifferencesInPrivateAndInternalVisibilitiesAReverse()<!> {}

class TestDifferencesInPrivateAndInternalVisibilitiesB {
    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) internal <!CONFLICTING_OVERLOADS!>constructor()<!>
}
<!CONFLICTING_OVERLOADS!>private fun TestDifferencesInPrivateAndInternalVisibilitiesB()<!> {}

class TestDifferencesInPrivateAndInternalVisibilitiesBReverse {
    internal <!CONFLICTING_OVERLOADS!>constructor()<!>
}
<!CONFLICTING_OVERLOADS!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) private fun TestDifferencesInPrivateAndInternalVisibilitiesBReverse()<!> {}


open class Invariant<T>


class UserKlass
class UserKlassA
class UserKlassB
typealias SameUserKlass = UserKlass
typealias SameUserKlassA = UserKlassA
typealias SameUserKlassB = UserKlassB


interface UserInterface
interface UserInterfaceA
interface UserInterfaceB
