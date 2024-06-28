// DIAGNOSTICS: -CONFLICTING_JVM_DECLARATIONS, -MISPLACED_TYPE_PARAMETER_CONSTRAINTS


class TestBasic {
    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) constructor()
    constructor()
}


class TestIdenticalPrimaryAndSecondaryConstructorsA constructor() {
    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) constructor() : this()
}

class TestIdenticalPrimaryAndSecondaryConstructorsB() {
    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) constructor() : this()
}


class TestIdenticalDelegatedPrimaryConstructorCalls constructor(placeholder: UserKlass) {
    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) constructor() : this(UserKlass())
    constructor() : this(UserKlass())
}


open class SuperConstructorSource constructor(placeholder: UserKlass)
class TestIdenticalDelegatedSuperConstructorCalls: SuperConstructorSource {
    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) constructor() : super(UserKlass())
    constructor() : super(UserKlass())
}


class TestIdenticalValueParameters {
    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) constructor(arg: UserKlass)
    constructor(arg: UserKlass)
}

class TestDifferentlyNamedValueParameters {
    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) constructor(argA: UserKlass)
    constructor(argB: UserKlass)
}

class TestTypeAliasedValueParameterTypes {
    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) constructor(arg: UserKlass)
    constructor(arg: SameUserKlass)
}

class TestTypeAliasedValueParameterTypesReverse {
    constructor(arg: UserKlass)
    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) constructor(arg: SameUserKlass)
}


class TestMultipleIdenticalValueParameters {
    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) constructor(arg1: UserKlassA, arg2: UserKlassB)
    constructor(arg1: UserKlassA, arg2: UserKlassB)
}

class TestMultipleDifferentlyNamedValueParametersA {
    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) constructor(arg1: UserKlassA, arg2A: UserKlassB)
    constructor(arg1: UserKlassA, arg2B: UserKlassB)
}

class TestMultipleDifferentlyNamedValueParametersB {
    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) constructor(arg1A: UserKlassA, arg2A: UserKlassB)
    constructor(arg1B: UserKlassA, arg2B: UserKlassB)
}

class TestMultipleTypeAliasedValueParameterTypesA {
    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) constructor(arg1: UserKlassA, arg2: UserKlassB)
    constructor(arg1: UserKlassA, arg2: SameUserKlassB)
}

class TestMultipleTypeAliasedValueParameterTypesAReverse {
    constructor(arg1: UserKlassA, arg2: UserKlassB)
    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) constructor(arg1: UserKlassA, arg2: SameUserKlassB)
}

class TestMultipleTypeAliasedValueParameterTypesB {
    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) constructor(arg1: UserKlassA, arg2: UserKlassB)
    constructor(arg1: SameUserKlassA, arg2: SameUserKlassB)
}

class TestMultipleTypeAliasedValueParameterTypesBReverse {
    constructor(arg1: UserKlassA, arg2: UserKlassB)
    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) constructor(arg1: SameUserKlassA, arg2: SameUserKlassB)
}


class TestValueParameterWithIdenticalDefaultArguments {
    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) constructor(arg: UserKlass = defaultArgument)
    constructor(arg: UserKlass = defaultArgument)
}

class TestDifferencesInValueParameterDefaultArgumentsPresence {
    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) constructor(arg: UserKlass = defaultArgument)
    constructor(arg: UserKlass)
}

class TestDifferencesInValueParameterDefaultArgumentsPresenceReverse {
    constructor(arg: UserKlass = defaultArgument)
    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) constructor(arg: UserKlass)
}

class TestValueParameterWithDifferentDefaultArguments {
    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) constructor(arg: UserKlass = defaultArgumentA)
    constructor(arg: UserKlass = defaultArgumentB)
}

class TestValueParameterWithAliasedDefaultArguments {
    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) constructor(arg: UserKlass = defaultArgument)
    constructor(arg: UserKlass = sameDefaultArgument)
}

class TestValueParameterWithAliasedDefaultArgumentsReverse {
    constructor(arg: UserKlass = defaultArgument)
    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) constructor(arg: UserKlass = sameDefaultArgument)
}


class TestIdenticalTypeParametersA<T> {
    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) constructor()
    constructor()
}

class TestIdenticalTypeParametersB<T> {
    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) constructor(arg: T)
    constructor(arg: T)
}

class TestIdenticalTypeParametersC<T> {
    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) constructor(arg: Invariant<T>)
    constructor(arg: Invariant<T>)
}


class TestMultipleIdenticalTypeParameters<T1, T2> {
    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) constructor()
    constructor()
}


class TestTypeParameterWithIdenticalUpperBoundsA<T: UserInterface> {
    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) constructor()
    constructor()
}

class TestTypeParameterWithIdenticalUpperBoundsB<T: UserInterface> {
    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) constructor(arg: T)
    constructor(arg: T)
}

class TestTypeParameterWithIdenticalUpperBoundsC<T: UserInterface> {
    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) constructor(arg: Invariant<T>)
    constructor(arg: Invariant<T>)
}


class TestTypeParameterWithMultipleIdenticalUpperBoundsAA<T> where T: UserInterfaceA, T: UserInterfaceB {
    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) constructor()
    constructor()
}

class TestTypeParameterWithMultipleIdenticalUpperBoundsAB<T> where T: UserInterfaceA, T: UserInterfaceB {
    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) constructor(arg: T)
    constructor(arg: T)
}

class TestTypeParameterWithMultipleIdenticalUpperBoundsAC<T> where T: UserInterfaceA, T: UserInterfaceB {
    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) constructor(arg: Invariant<T>)
    constructor(arg: Invariant<T>)
}

class TestTypeParameterWithMultipleIdenticalUpperBoundsBA<T: UserInterfaceA> where T: UserInterfaceB {
    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) constructor()
    constructor()
}

class TestTypeParameterWithMultipleIdenticalUpperBoundsBB<T: UserInterfaceA> where T: UserInterfaceB {
    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) constructor(arg: T)
    constructor(arg: T)
}

class TestTypeParameterWithMultipleIdenticalUpperBoundsBC<T: UserInterfaceA> where T: UserInterfaceB {
    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) constructor(arg: Invariant<T>)
    constructor(arg: Invariant<T>)
}


class TestIdenticalPrivateVisibility {
    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) private constructor()
    private constructor()
}

class TestIdenticalInternalVisibility {
    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) internal constructor()
    internal constructor()
}

open class TestIdenticalProtectedVisibility {
    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) protected constructor()
    protected constructor()
}

class TestDifferencesInPrivateAndPublicVisibilities {
    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) private constructor()
    public constructor()
}

class TestDifferencesInPrivateAndPublicVisibilitiesReverse {
    private constructor()
    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) public constructor()
}

class TestDifferencesInInternalAndPublicVisibilities {
    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) internal constructor()
    public constructor()
}

class TestDifferencesInInternalAndPublicVisibilitiesReverse {
    internal constructor()
    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) public constructor()
}

open class TestDifferencesInProtectedAndPublicVisibilities {
    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) protected constructor()
    public constructor()
}

open class TestDifferencesInProtectedAndPublicVisibilitiesReverse {
    protected constructor()
    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) public constructor()
}

class TestDifferencesInPrivateAndInternalVisibilities {
    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) private constructor()
    internal constructor()
}

class TestDifferencesInPrivateAndInternalVisibilitiesReverse {
    private constructor()
    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) internal constructor()
}

open class TestDifferencesInProtectedAndPrivateVisibilities {
    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) protected constructor()
    private constructor()
}

open class TestDifferencesInProtectedAndPrivateVisibilitiesReverse {
    protected constructor()
    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) private constructor()
}

open class TestDifferencesInProtectedAndInternalVisibilities {
    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) protected constructor()
    internal constructor()
}

open class TestDifferencesInProtectedAndInternalVisibilitiesReverse {
    protected constructor()
    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) internal constructor()
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
