// DIAGNOSTICS: -NOTHING_TO_INLINE, -NO_TAIL_CALLS_FOUND, -MISPLACED_TYPE_PARAMETER_CONSTRAINTS

package pkg


class TestBasic {
    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) constructor()
}
fun TestBasic() {}

class TestBasicReverse {
    constructor()
}
@Deprecated(message = "", level = DeprecationLevel.HIDDEN) fun TestBasicReverse() {}


class TestIdenticalReturnTypes {
    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) constructor()
}
fun TestIdenticalReturnTypes(): TestIdenticalReturnTypes = TestIdenticalReturnTypes()

class TestIdenticalReturnTypesReverse {
    constructor()
}
@Deprecated(message = "", level = DeprecationLevel.HIDDEN) fun TestIdenticalReturnTypesReverse(): TestIdenticalReturnTypesReverse = TestIdenticalReturnTypesReverse()


class TestFunctionWithReifiedTypeParameterVsConstructorA<T> {
    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) constructor()
}
inline fun <reified T> TestFunctionWithReifiedTypeParameterVsConstructorA() {}

class TestFunctionWithReifiedTypeParameterVsConstructorAReverse<T> {
    constructor()
}
@Deprecated(message = "", level = DeprecationLevel.HIDDEN) inline fun <reified T> TestFunctionWithReifiedTypeParameterVsConstructorAReverse() {}

class TestFunctionWithReifiedTypeParameterVsConstructorB<T> {
    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) constructor(arg: T)
}
inline fun <reified T> TestFunctionWithReifiedTypeParameterVsConstructorB(arg: T) {}

class TestFunctionWithReifiedTypeParameterVsConstructorBReverse<T> {
    constructor(arg: T)
}
@Deprecated(message = "", level = DeprecationLevel.HIDDEN) inline fun <reified T> TestFunctionWithReifiedTypeParameterVsConstructorBReverse(arg: T) {}

class TestFunctionWithReifiedTypeParameterVsConstructorC<T> {
    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) constructor(arg: Invariant<T>)
}
inline fun <reified T> TestFunctionWithReifiedTypeParameterVsConstructorC(arg: Invariant<T>) {}

class TestFunctionWithReifiedTypeParameterVsConstructorCReverse<T> {
    constructor(arg: Invariant<T>)
}
@Deprecated(message = "", level = DeprecationLevel.HIDDEN) inline fun <reified T> TestFunctionWithReifiedTypeParameterVsConstructorCReverse(arg: Invariant<T>) {}


class TestInlineFunctionVsConstructor {
    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) constructor()
}
inline fun TestInlineFunctionVsConstructor() {}

class TestInlineFunctionVsConstructorReverse {
    constructor()
}
@Deprecated(message = "", level = DeprecationLevel.HIDDEN) inline fun TestInlineFunctionVsConstructorReverse() {}


class TestTailrecFunctionVsConstructor {
    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) constructor()
}
tailrec fun TestTailrecFunctionVsConstructor() {}

class TestTailrecFunctionVsConstructorReverse {
    constructor()
}
@Deprecated(message = "", level = DeprecationLevel.HIDDEN) tailrec fun TestTailrecFunctionVsConstructorReverse() {}


class TestFunctionVsPrimaryConstructor @Deprecated(message = "", level = DeprecationLevel.HIDDEN) constructor()
fun TestFunctionVsPrimaryConstructor() {}

class TestFunctionVsPrimaryConstructorReverse constructor() {}
@Deprecated(message = "", level = DeprecationLevel.HIDDEN) fun TestFunctionVsPrimaryConstructorReverse() {}


class TestFunctionVsDelegatedPrimaryConstructorCall constructor(placeholder: UserKlass) {
    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) constructor() : this(UserKlass())
}
fun TestFunctionVsDelegatedPrimaryConstructorCall() {}

class TestFunctionVsDelegatedPrimaryConstructorCallReverse constructor(placeholder: UserKlass) {
    constructor() : this(UserKlass())
}
@Deprecated(message = "", level = DeprecationLevel.HIDDEN) fun TestFunctionVsDelegatedPrimaryConstructorCallReverse() {}


open class SuperConstructorSource constructor(placeholder: UserKlass)

class TestFunctionVsDelegatedSuperConstructorCall: SuperConstructorSource {
    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) constructor() : super(UserKlass())
}
fun TestFunctionVsDelegatedSuperConstructorCall() {}

class TestFunctionVsDelegatedSuperConstructorCallReverse: SuperConstructorSource {
    constructor() : super(UserKlass())
}
@Deprecated(message = "", level = DeprecationLevel.HIDDEN) fun TestFunctionVsDelegatedSuperConstructorCallReverse() {}


class TestIdenticalValueParameters {
    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) constructor(arg: UserKlass)
}
fun TestIdenticalValueParameters(arg: UserKlass) {}

class TestIdenticalValueParametersReverse {
    constructor(arg: UserKlass)
}
@Deprecated(message = "", level = DeprecationLevel.HIDDEN) fun TestIdenticalValueParametersReverse(arg: UserKlass) {}

class TestDifferentlyNamedValueParameters {
    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) constructor(argA: UserKlass)
}
fun TestDifferentlyNamedValueParameters(argB: UserKlass) {}

class TestDifferentlyNamedValueParametersReverse {
    constructor(argA: UserKlass)
}
@Deprecated(message = "", level = DeprecationLevel.HIDDEN) fun TestDifferentlyNamedValueParametersReverse(argB: UserKlass) {}

class TestTypeAliasedValueParameterTypesA {
    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) constructor(arg: UserKlass)
}
fun TestTypeAliasedValueParameterTypesA(arg: SameUserKlass) {}

class TestTypeAliasedValueParameterTypesAReverse {
    constructor(arg: UserKlass)
}
@Deprecated(message = "", level = DeprecationLevel.HIDDEN) fun TestTypeAliasedValueParameterTypesAReverse(arg: SameUserKlass) {}

class TestTypeAliasedValueParameterTypesB {
    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) constructor(arg: SameUserKlass)
}
fun TestTypeAliasedValueParameterTypesB(arg: UserKlass) {}

class TestTypeAliasedValueParameterTypesBReverse {
    constructor(arg: SameUserKlass)
}
@Deprecated(message = "", level = DeprecationLevel.HIDDEN) fun TestTypeAliasedValueParameterTypesBReverse(arg: UserKlass) {}


class TestMultipleIdenticalValueParameters {
    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) constructor(arg1: UserKlassA, arg2: UserKlassB)
}
fun TestMultipleIdenticalValueParameters(arg1: UserKlassA, arg2: UserKlassB) {}

class TestMultipleIdenticalValueParametersReverse {
    constructor(arg1: UserKlassA, arg2: UserKlassB)
}
@Deprecated(message = "", level = DeprecationLevel.HIDDEN) fun TestMultipleIdenticalValueParametersReverse(arg1: UserKlassA, arg2: UserKlassB) {}

class TestMultipleDifferentlyNamedValueParametersA {
    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) constructor(arg1: UserKlassA, arg2A: UserKlassB)
}
fun TestMultipleDifferentlyNamedValueParametersA(arg1: UserKlassA, arg2B: UserKlassB) {}

class TestMultipleDifferentlyNamedValueParametersAReverse {
    constructor(arg1: UserKlassA, arg2A: UserKlassB)
}
@Deprecated(message = "", level = DeprecationLevel.HIDDEN) fun TestMultipleDifferentlyNamedValueParametersAReverse(arg1: UserKlassA, arg2B: UserKlassB) {}

class TestMultipleDifferentlyNamedValueParametersB {
    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) constructor(arg1A: UserKlassA, arg2A: UserKlassB)
}
fun TestMultipleDifferentlyNamedValueParametersB(arg1B: UserKlassA, arg2B: UserKlassB) {}

class TestMultipleDifferentlyNamedValueParametersBReverse {
    constructor(arg1A: UserKlassA, arg2A: UserKlassB)
}
@Deprecated(message = "", level = DeprecationLevel.HIDDEN) fun TestMultipleDifferentlyNamedValueParametersBReverse(arg1B: UserKlassA, arg2B: UserKlassB) {}

class TestMultipleTypeAliasedValueParameterTypesA {
    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) constructor(arg1: UserKlassA, arg2: SameUserKlassB)
}
fun TestMultipleTypeAliasedValueParameterTypesA(arg1: UserKlassA, arg2: SameUserKlassB) {}

class TestMultipleTypeAliasedValueParameterTypesAReverse {
    constructor(arg1: UserKlassA, arg2: SameUserKlassB)
}
@Deprecated(message = "", level = DeprecationLevel.HIDDEN) fun TestMultipleTypeAliasedValueParameterTypesAReverse(arg1: UserKlassA, arg2: SameUserKlassB) {}

class TestMultipleTypeAliasedValueParameterTypesB {
    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) constructor(arg1: SameUserKlassA, arg2: SameUserKlassB)
}
fun TestMultipleTypeAliasedValueParameterTypesB(arg1: SameUserKlassA, arg2: SameUserKlassB) {}

class TestMultipleTypeAliasedValueParameterTypesBReverse {
    constructor(arg1: SameUserKlassA, arg2: SameUserKlassB)
}
@Deprecated(message = "", level = DeprecationLevel.HIDDEN) fun TestMultipleTypeAliasedValueParameterTypesBReverse(arg1: SameUserKlassA, arg2: SameUserKlassB) {}


class TestIdenticalTypeParametersA<T> {
    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) constructor()
}
fun <T> TestIdenticalTypeParametersA() {}

class TestIdenticalTypeParametersAReverse<T> {
    constructor()
}
@Deprecated(message = "", level = DeprecationLevel.HIDDEN) fun <T> TestIdenticalTypeParametersAReverse() {}

class TestIdenticalTypeParametersB<T> {
    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) constructor(arg: T)
}
fun <T> TestIdenticalTypeParametersB(arg: T) {}

class TestIdenticalTypeParametersBReverse<T> {
    constructor(arg: T)
}
@Deprecated(message = "", level = DeprecationLevel.HIDDEN) fun <T> TestIdenticalTypeParametersBReverse(arg: T) {}

class TestIdenticalTypeParametersC<T> {
    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) constructor(arg: Invariant<T>)
}
fun <T> TestIdenticalTypeParametersC(arg: Invariant<T>) {}

class TestIdenticalTypeParametersCReverse<T> {
    constructor(arg: Invariant<T>)
}
@Deprecated(message = "", level = DeprecationLevel.HIDDEN) fun <T> TestIdenticalTypeParametersCReverse(arg: Invariant<T>) {}


class TestMultipleIdenticalTypeParameters<T1, T2> {
    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) constructor()
}
fun <T1, T2> TestMultipleIdenticalTypeParameters() {}

class TestMultipleIdenticalTypeParametersReverse<T1, T2> {
    constructor()
}
@Deprecated(message = "", level = DeprecationLevel.HIDDEN) fun <T1, T2> TestMultipleIdenticalTypeParametersReverse() {}


class TestTypeParameterWithIdenticalUpperBoundsA<T: UserInterface> {
    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) constructor()
}
fun <T: UserInterface> TestTypeParameterWithIdenticalUpperBoundsA() {}

class TestTypeParameterWithIdenticalUpperBoundsAReverse<T: UserInterface> {
    constructor()
}
@Deprecated(message = "", level = DeprecationLevel.HIDDEN) fun <T: UserInterface> TestTypeParameterWithIdenticalUpperBoundsAReverse() {}

class TestTypeParameterWithIdenticalUpperBoundsB<T: UserInterface> {
    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) constructor(arg: T)
}
fun <T: UserInterface> TestTypeParameterWithIdenticalUpperBoundsB(arg: T) {}

class TestTypeParameterWithIdenticalUpperBoundsBReverse<T: UserInterface> {
    constructor(arg: T)
}
@Deprecated(message = "", level = DeprecationLevel.HIDDEN) fun <T: UserInterface> TestTypeParameterWithIdenticalUpperBoundsBReverse(arg: T) {}

class TestTypeParameterWithIdenticalUpperBoundsC<T: UserInterface> {
    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) constructor(arg: Invariant<T>)
}
fun <T: UserInterface> TestTypeParameterWithIdenticalUpperBoundsC(arg: Invariant<T>) {}

class TestTypeParameterWithIdenticalUpperBoundsCReverse<T: UserInterface> {
    constructor(arg: Invariant<T>)
}
@Deprecated(message = "", level = DeprecationLevel.HIDDEN) fun <T: UserInterface> TestTypeParameterWithIdenticalUpperBoundsCReverse(arg: Invariant<T>) {}


class TestTypeParameterWithMultipleIdenticalUpperBoundsAA<T> where T: UserInterfaceA, T: UserInterfaceB {
    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) constructor()
}
fun <T> TestTypeParameterWithMultipleIdenticalUpperBoundsAA() where T: UserInterfaceA, T: UserInterfaceB {}

class TestTypeParameterWithMultipleIdenticalUpperBoundsAAReverse<T> where T: UserInterfaceA, T: UserInterfaceB {
    constructor()
}
@Deprecated(message = "", level = DeprecationLevel.HIDDEN) fun <T> TestTypeParameterWithMultipleIdenticalUpperBoundsAAReverse() where T: UserInterfaceA, T: UserInterfaceB {}

class TestTypeParameterWithMultipleIdenticalUpperBoundsAB<T> where T: UserInterfaceA, T: UserInterfaceB {
    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) constructor(arg: T)
}
fun <T> TestTypeParameterWithMultipleIdenticalUpperBoundsAB(arg: T) where T: UserInterfaceA, T: UserInterfaceB {}

class TestTypeParameterWithMultipleIdenticalUpperBoundsABReverse<T> where T: UserInterfaceA, T: UserInterfaceB {
    constructor(arg: T)
}
@Deprecated(message = "", level = DeprecationLevel.HIDDEN) fun <T> TestTypeParameterWithMultipleIdenticalUpperBoundsABReverse(arg: T) where T: UserInterfaceA, T: UserInterfaceB {}

class TestTypeParameterWithMultipleIdenticalUpperBoundsAC<T> where T: UserInterfaceA, T: UserInterfaceB {
    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) constructor(arg: Invariant<T>)
}
fun <T> TestTypeParameterWithMultipleIdenticalUpperBoundsAC(arg: Invariant<T>) where T: UserInterfaceA, T: UserInterfaceB {}

class TestTypeParameterWithMultipleIdenticalUpperBoundsACReverse<T> where T: UserInterfaceA, T: UserInterfaceB {
    constructor(arg: Invariant<T>)
}
@Deprecated(message = "", level = DeprecationLevel.HIDDEN) fun <T> TestTypeParameterWithMultipleIdenticalUpperBoundsACReverse(arg: Invariant<T>) where T: UserInterfaceA, T: UserInterfaceB {}

class TestTypeParameterWithMultipleIdenticalUpperBoundsBA<T: UserInterfaceA> where T: UserInterfaceB {
    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) constructor()
}
fun <T: UserInterfaceA> TestTypeParameterWithMultipleIdenticalUpperBoundsBA() where T: UserInterfaceB {}

class TestTypeParameterWithMultipleIdenticalUpperBoundsBAReverse<T: UserInterfaceA> where T: UserInterfaceB {
    constructor()
}
@Deprecated(message = "", level = DeprecationLevel.HIDDEN) fun <T: UserInterfaceA> TestTypeParameterWithMultipleIdenticalUpperBoundsBAReverse() where T: UserInterfaceB {}

class TestTypeParameterWithMultipleIdenticalUpperBoundsBB<T: UserInterfaceA> where T: UserInterfaceB {
    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) constructor(arg: T)
}
fun <T: UserInterfaceA> TestTypeParameterWithMultipleIdenticalUpperBoundsBB(arg: T) where T: UserInterfaceB {}

class TestTypeParameterWithMultipleIdenticalUpperBoundsBBReverse<T: UserInterfaceA> where T: UserInterfaceB {
    constructor(arg: T)
}
@Deprecated(message = "", level = DeprecationLevel.HIDDEN) fun <T: UserInterfaceA> TestTypeParameterWithMultipleIdenticalUpperBoundsBBReverse(arg: T) where T: UserInterfaceB {}

class TestTypeParameterWithMultipleIdenticalUpperBoundsBC<T: UserInterfaceA> where T: UserInterfaceB {
    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) constructor(arg: Invariant<T>)
}
fun <T: UserInterfaceA> TestTypeParameterWithMultipleIdenticalUpperBoundsBC(arg: Invariant<T>) where T: UserInterfaceB {}

class TestTypeParameterWithMultipleIdenticalUpperBoundsBCReverse<T: UserInterfaceA> where T: UserInterfaceB {
    constructor(arg: Invariant<T>)
}
@Deprecated(message = "", level = DeprecationLevel.HIDDEN) fun <T: UserInterfaceA> TestTypeParameterWithMultipleIdenticalUpperBoundsBCReverse(arg: Invariant<T>) where T: UserInterfaceB {}


class TestIdenticalPrivateVisibility {
    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) private constructor()
}
private fun TestIdenticalPrivateVisibility() {}

class TestIdenticalPrivateVisibilityReverse {
    private constructor()
}
@Deprecated(message = "", level = DeprecationLevel.HIDDEN) private fun TestIdenticalPrivateVisibilityReverse() {}

class TestIdenticalInternalVisibility {
    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) internal constructor()
}
internal fun TestIdenticalInternalVisibility() {}

class TestIdenticalInternalVisibilityReverse {
    internal constructor()
}
@Deprecated(message = "", level = DeprecationLevel.HIDDEN) internal fun TestIdenticalInternalVisibilityReverse() {}

class TestDifferencesInPrivateAndPublicVisibilitiesA {
    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) private constructor()
}
public fun TestDifferencesInPrivateAndPublicVisibilitiesA() {}

class TestDifferencesInPrivateAndPublicVisibilitiesAReverse {
    private constructor()
}
@Deprecated(message = "", level = DeprecationLevel.HIDDEN) public fun TestDifferencesInPrivateAndPublicVisibilitiesAReverse() {}

class TestDifferencesInPrivateAndPublicVisibilitiesB {
    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) public constructor()
}
private fun TestDifferencesInPrivateAndPublicVisibilitiesB() {}

class TestDifferencesInPrivateAndPublicVisibilitiesBReverse {
    public constructor()
}
@Deprecated(message = "", level = DeprecationLevel.HIDDEN) private fun TestDifferencesInPrivateAndPublicVisibilitiesBReverse() {}

class TestDifferencesInInternalAndPublicVisibilitiesA {
    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) internal constructor()
}
public fun TestDifferencesInInternalAndPublicVisibilitiesA() {}

class TestDifferencesInInternalAndPublicVisibilitiesAReverse {
    internal constructor()
}
@Deprecated(message = "", level = DeprecationLevel.HIDDEN) public fun TestDifferencesInInternalAndPublicVisibilitiesAReverse() {}

class TestDifferencesInInternalAndPublicVisibilitiesB {
    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) public constructor()
}
internal fun TestDifferencesInInternalAndPublicVisibilitiesB() {}

class TestDifferencesInInternalAndPublicVisibilitiesBReverse {
    public constructor()
}
@Deprecated(message = "", level = DeprecationLevel.HIDDEN) internal fun TestDifferencesInInternalAndPublicVisibilitiesBReverse() {}

class TestDifferencesInPrivateAndInternalVisibilitiesA {
    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) private constructor()
}
internal fun TestDifferencesInPrivateAndInternalVisibilitiesA() {}

class TestDifferencesInPrivateAndInternalVisibilitiesAReverse {
    private constructor()
}
@Deprecated(message = "", level = DeprecationLevel.HIDDEN) internal fun TestDifferencesInPrivateAndInternalVisibilitiesAReverse() {}

class TestDifferencesInPrivateAndInternalVisibilitiesB {
    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) internal constructor()
}
private fun TestDifferencesInPrivateAndInternalVisibilitiesB() {}

class TestDifferencesInPrivateAndInternalVisibilitiesBReverse {
    internal constructor()
}
@Deprecated(message = "", level = DeprecationLevel.HIDDEN) private fun TestDifferencesInPrivateAndInternalVisibilitiesBReverse() {}


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
