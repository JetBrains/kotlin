// DIAGNOSTICS: -CONFLICTING_JVM_DECLARATIONS, -MISPLACED_TYPE_PARAMETER_CONSTRAINTS

package pkg


class TestBasic {
    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) <!CONFLICTING_OVERLOADS!>constructor()<!>
    <!CONFLICTING_OVERLOADS!>constructor()<!>
}


class TestIdenticalPrimaryAndSecondaryConstructorsA <!CONFLICTING_OVERLOADS!>constructor()<!> {
    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) <!CONFLICTING_OVERLOADS!>constructor()<!> : this()
}

class TestIdenticalPrimaryAndSecondaryConstructorsB<!CONFLICTING_OVERLOADS!>()<!> {
    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) <!CONFLICTING_OVERLOADS!>constructor()<!> : this()
}


class TestIdenticalDelegatedPrimaryConstructorCalls constructor(placeholder: UserKlass) {
    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) <!CONFLICTING_OVERLOADS!>constructor()<!> : this(UserKlass())
    <!CONFLICTING_OVERLOADS!>constructor()<!> : this(UserKlass())
}


open class SuperConstructorSource constructor(placeholder: UserKlass)
class TestIdenticalDelegatedSuperConstructorCalls: SuperConstructorSource {
    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) <!CONFLICTING_OVERLOADS!>constructor()<!> : super(UserKlass())
    <!CONFLICTING_OVERLOADS!>constructor()<!> : super(UserKlass())
}


class TestIdenticalValueParameters {
    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) <!CONFLICTING_OVERLOADS!>constructor(arg: UserKlass)<!>
    <!CONFLICTING_OVERLOADS!>constructor(arg: UserKlass)<!>
}

class TestDifferentlyNamedValueParameters {
    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) <!CONFLICTING_OVERLOADS!>constructor(argA: UserKlass)<!>
    <!CONFLICTING_OVERLOADS!>constructor(argB: UserKlass)<!>
}

class TestTypeAliasedValueParameterTypes {
    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) <!CONFLICTING_OVERLOADS!>constructor(arg: UserKlass)<!>
    <!CONFLICTING_OVERLOADS!>constructor(arg: SameUserKlass)<!>
}

class TestTypeAliasedValueParameterTypesReverse {
    <!CONFLICTING_OVERLOADS!>constructor(arg: UserKlass)<!>
    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) <!CONFLICTING_OVERLOADS!>constructor(arg: SameUserKlass)<!>
}


class TestMultipleIdenticalValueParameters {
    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) <!CONFLICTING_OVERLOADS!>constructor(arg1: UserKlassA, arg2: UserKlassB)<!>
    <!CONFLICTING_OVERLOADS!>constructor(arg1: UserKlassA, arg2: UserKlassB)<!>
}

class TestMultipleDifferentlyNamedValueParametersA {
    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) <!CONFLICTING_OVERLOADS!>constructor(arg1: UserKlassA, arg2A: UserKlassB)<!>
    <!CONFLICTING_OVERLOADS!>constructor(arg1: UserKlassA, arg2B: UserKlassB)<!>
}

class TestMultipleDifferentlyNamedValueParametersB {
    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) <!CONFLICTING_OVERLOADS!>constructor(arg1A: UserKlassA, arg2A: UserKlassB)<!>
    <!CONFLICTING_OVERLOADS!>constructor(arg1B: UserKlassA, arg2B: UserKlassB)<!>
}

class TestMultipleTypeAliasedValueParameterTypesA {
    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) <!CONFLICTING_OVERLOADS!>constructor(arg1: UserKlassA, arg2: UserKlassB)<!>
    <!CONFLICTING_OVERLOADS!>constructor(arg1: UserKlassA, arg2: SameUserKlassB)<!>
}

class TestMultipleTypeAliasedValueParameterTypesAReverse {
    <!CONFLICTING_OVERLOADS!>constructor(arg1: UserKlassA, arg2: UserKlassB)<!>
    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) <!CONFLICTING_OVERLOADS!>constructor(arg1: UserKlassA, arg2: SameUserKlassB)<!>
}

class TestMultipleTypeAliasedValueParameterTypesB {
    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) <!CONFLICTING_OVERLOADS!>constructor(arg1: UserKlassA, arg2: UserKlassB)<!>
    <!CONFLICTING_OVERLOADS!>constructor(arg1: SameUserKlassA, arg2: SameUserKlassB)<!>
}

class TestMultipleTypeAliasedValueParameterTypesBReverse {
    <!CONFLICTING_OVERLOADS!>constructor(arg1: UserKlassA, arg2: UserKlassB)<!>
    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) <!CONFLICTING_OVERLOADS!>constructor(arg1: SameUserKlassA, arg2: SameUserKlassB)<!>
}


class TestValueParameterWithIdenticalDefaultArguments {
    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) <!CONFLICTING_OVERLOADS!>constructor(arg: UserKlass = defaultArgument)<!>
    <!CONFLICTING_OVERLOADS!>constructor(arg: UserKlass = defaultArgument)<!>
}

class TestDifferencesInValueParameterDefaultArgumentsPresence {
    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) <!CONFLICTING_OVERLOADS!>constructor(arg: UserKlass = defaultArgument)<!>
    <!CONFLICTING_OVERLOADS!>constructor(arg: UserKlass)<!>
}

class TestDifferencesInValueParameterDefaultArgumentsPresenceReverse {
    <!CONFLICTING_OVERLOADS!>constructor(arg: UserKlass = defaultArgument)<!>
    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) <!CONFLICTING_OVERLOADS!>constructor(arg: UserKlass)<!>
}

class TestValueParameterWithDifferentDefaultArguments {
    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) <!CONFLICTING_OVERLOADS!>constructor(arg: UserKlass = defaultArgumentA)<!>
    <!CONFLICTING_OVERLOADS!>constructor(arg: UserKlass = defaultArgumentB)<!>
}

class TestValueParameterWithAliasedDefaultArguments {
    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) <!CONFLICTING_OVERLOADS!>constructor(arg: UserKlass = defaultArgument)<!>
    <!CONFLICTING_OVERLOADS!>constructor(arg: UserKlass = sameDefaultArgument)<!>
}

class TestValueParameterWithAliasedDefaultArgumentsReverse {
    <!CONFLICTING_OVERLOADS!>constructor(arg: UserKlass = defaultArgument)<!>
    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) <!CONFLICTING_OVERLOADS!>constructor(arg: UserKlass = sameDefaultArgument)<!>
}


class TestIdenticalTypeParametersA<T> {
    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) <!CONFLICTING_OVERLOADS!>constructor()<!>
    <!CONFLICTING_OVERLOADS!>constructor()<!>
}

class TestIdenticalTypeParametersB<T> {
    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) <!CONFLICTING_OVERLOADS!>constructor(arg: T)<!>
    <!CONFLICTING_OVERLOADS!>constructor(arg: T)<!>
}

class TestIdenticalTypeParametersC<T> {
    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) <!CONFLICTING_OVERLOADS!>constructor(arg: Invariant<T>)<!>
    <!CONFLICTING_OVERLOADS!>constructor(arg: Invariant<T>)<!>
}


class TestMultipleIdenticalTypeParameters<T1, T2> {
    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) <!CONFLICTING_OVERLOADS!>constructor()<!>
    <!CONFLICTING_OVERLOADS!>constructor()<!>
}


class TestTypeParameterWithIdenticalUpperBoundsA<T: UserInterface> {
    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) <!CONFLICTING_OVERLOADS!>constructor()<!>
    <!CONFLICTING_OVERLOADS!>constructor()<!>
}

class TestTypeParameterWithIdenticalUpperBoundsB<T: UserInterface> {
    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) <!CONFLICTING_OVERLOADS!>constructor(arg: T)<!>
    <!CONFLICTING_OVERLOADS!>constructor(arg: T)<!>
}

class TestTypeParameterWithIdenticalUpperBoundsC<T: UserInterface> {
    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) <!CONFLICTING_OVERLOADS!>constructor(arg: Invariant<T>)<!>
    <!CONFLICTING_OVERLOADS!>constructor(arg: Invariant<T>)<!>
}


class TestTypeParameterWithMultipleIdenticalUpperBoundsAA<T> where T: UserInterfaceA, T: UserInterfaceB {
    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) <!CONFLICTING_OVERLOADS!>constructor()<!>
    <!CONFLICTING_OVERLOADS!>constructor()<!>
}

class TestTypeParameterWithMultipleIdenticalUpperBoundsAB<T> where T: UserInterfaceA, T: UserInterfaceB {
    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) <!CONFLICTING_OVERLOADS!>constructor(arg: T)<!>
    <!CONFLICTING_OVERLOADS!>constructor(arg: T)<!>
}

class TestTypeParameterWithMultipleIdenticalUpperBoundsAC<T> where T: UserInterfaceA, T: UserInterfaceB {
    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) <!CONFLICTING_OVERLOADS!>constructor(arg: Invariant<T>)<!>
    <!CONFLICTING_OVERLOADS!>constructor(arg: Invariant<T>)<!>
}

class TestTypeParameterWithMultipleIdenticalUpperBoundsBA<T: UserInterfaceA> where T: UserInterfaceB {
    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) <!CONFLICTING_OVERLOADS!>constructor()<!>
    <!CONFLICTING_OVERLOADS!>constructor()<!>
}

class TestTypeParameterWithMultipleIdenticalUpperBoundsBB<T: UserInterfaceA> where T: UserInterfaceB {
    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) <!CONFLICTING_OVERLOADS!>constructor(arg: T)<!>
    <!CONFLICTING_OVERLOADS!>constructor(arg: T)<!>
}

class TestTypeParameterWithMultipleIdenticalUpperBoundsBC<T: UserInterfaceA> where T: UserInterfaceB {
    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) <!CONFLICTING_OVERLOADS!>constructor(arg: Invariant<T>)<!>
    <!CONFLICTING_OVERLOADS!>constructor(arg: Invariant<T>)<!>
}


class TestIdenticalPrivateVisibility {
    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) private <!CONFLICTING_OVERLOADS!>constructor()<!>
    private <!CONFLICTING_OVERLOADS!>constructor()<!>
}

class TestIdenticalInternalVisibility {
    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) internal <!CONFLICTING_OVERLOADS!>constructor()<!>
    internal <!CONFLICTING_OVERLOADS!>constructor()<!>
}

open class TestIdenticalProtectedVisibility {
    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) protected <!CONFLICTING_OVERLOADS!>constructor()<!>
    protected <!CONFLICTING_OVERLOADS!>constructor()<!>
}

class TestDifferencesInPrivateAndPublicVisibilities {
    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) private <!CONFLICTING_OVERLOADS!>constructor()<!>
    public <!CONFLICTING_OVERLOADS!>constructor()<!>
}

class TestDifferencesInPrivateAndPublicVisibilitiesReverse {
    private <!CONFLICTING_OVERLOADS!>constructor()<!>
    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) public <!CONFLICTING_OVERLOADS!>constructor()<!>
}

class TestDifferencesInInternalAndPublicVisibilities {
    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) internal <!CONFLICTING_OVERLOADS!>constructor()<!>
    public <!CONFLICTING_OVERLOADS!>constructor()<!>
}

class TestDifferencesInInternalAndPublicVisibilitiesReverse {
    internal <!CONFLICTING_OVERLOADS!>constructor()<!>
    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) public <!CONFLICTING_OVERLOADS!>constructor()<!>
}

open class TestDifferencesInProtectedAndPublicVisibilities {
    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) protected <!CONFLICTING_OVERLOADS!>constructor()<!>
    public <!CONFLICTING_OVERLOADS!>constructor()<!>
}

open class TestDifferencesInProtectedAndPublicVisibilitiesReverse {
    protected <!CONFLICTING_OVERLOADS!>constructor()<!>
    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) public <!CONFLICTING_OVERLOADS!>constructor()<!>
}

class TestDifferencesInPrivateAndInternalVisibilities {
    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) private <!CONFLICTING_OVERLOADS!>constructor()<!>
    internal <!CONFLICTING_OVERLOADS!>constructor()<!>
}

class TestDifferencesInPrivateAndInternalVisibilitiesReverse {
    private <!CONFLICTING_OVERLOADS!>constructor()<!>
    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) internal <!CONFLICTING_OVERLOADS!>constructor()<!>
}

open class TestDifferencesInProtectedAndPrivateVisibilities {
    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) protected <!CONFLICTING_OVERLOADS!>constructor()<!>
    private <!CONFLICTING_OVERLOADS!>constructor()<!>
}

open class TestDifferencesInProtectedAndPrivateVisibilitiesReverse {
    protected <!CONFLICTING_OVERLOADS!>constructor()<!>
    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) private <!CONFLICTING_OVERLOADS!>constructor()<!>
}

open class TestDifferencesInProtectedAndInternalVisibilities {
    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) protected <!CONFLICTING_OVERLOADS!>constructor()<!>
    internal <!CONFLICTING_OVERLOADS!>constructor()<!>
}

open class TestDifferencesInProtectedAndInternalVisibilitiesReverse {
    protected <!CONFLICTING_OVERLOADS!>constructor()<!>
    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) internal <!CONFLICTING_OVERLOADS!>constructor()<!>
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
