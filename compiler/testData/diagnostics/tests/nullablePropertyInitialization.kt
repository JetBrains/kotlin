// RUN_PIPELINE_TILL: FRONTEND
// DIAGNOSTICS: -UNUSED_PARAMETER -UNUSED_VARIABLE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE

// Test that nullable properties don't require initialization

class TestClass {
    // Nullable properties should not require initialization
    var nullableProperty1: String?
    val nullableProperty2: Int?
    
    // Non-nullable properties should still require initialization
    <!MUST_BE_INITIALIZED_OR_BE_ABSTRACT!>var nonNullableProperty1: String<!>
    <!MUST_BE_INITIALIZED_OR_BE_ABSTRACT!>val nonNullableProperty2: Int<!>
    
    // Initialized properties should be fine
    var initializedNullable: String? = null
    val initializedNonNullable: Int = 42
}

// Top-level properties
// Nullable properties should not require initialization
var topLevelNullableProperty1: String?
val topLevelNullableProperty2: Int?

// Non-nullable properties should still require initialization
<!MUST_BE_INITIALIZED!>var topLevelNonNullableProperty1: String<!>
<!MUST_BE_INITIALIZED!>val topLevelNonNullableProperty2: Int<!>

// Initialized properties should be fine
var topLevelInitializedNullable: String? = null
val topLevelInitializedNonNullable: Int = 42