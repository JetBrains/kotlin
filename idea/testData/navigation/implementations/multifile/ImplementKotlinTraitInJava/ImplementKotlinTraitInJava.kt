package testing.kt

trait Base

trait Derived: <caret>Base

// REF: (testing.jj).JavaClass
// REF: (testing.jj).JavaInterface
// REF: (testing.kt).Derived