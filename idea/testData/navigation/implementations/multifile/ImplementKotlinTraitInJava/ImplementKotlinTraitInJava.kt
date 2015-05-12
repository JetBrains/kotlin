package testing.kt

interface Base

interface Derived: <caret>Base

// REF: (testing.jj).JavaClass
// REF: (testing.jj).JavaInterface
// REF: (testing.kt).Derived