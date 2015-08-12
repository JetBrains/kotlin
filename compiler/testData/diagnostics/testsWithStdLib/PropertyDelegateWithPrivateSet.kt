// See KT-10107: 'Variable must be initialized' for delegate with private set

class My {
    var delegate: String by kotlin.properties.Delegates.notNull()
        private set

    // Error: Variable 'delegate' must be initialized
    val another: String = <!DEBUG_INFO_LEAKING_THIS!>delegate<!>

    var delegateWithBackingField: String by kotlin.properties.Delegates.notNull()
        <!ACCESSOR_FOR_DELEGATED_PROPERTY!>private set(arg) { field = arg }<!>
}
