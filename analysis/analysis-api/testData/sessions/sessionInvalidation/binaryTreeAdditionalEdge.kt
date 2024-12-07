// MODULE: L1
// MODULE_KIND: LibraryBinary
// FILE: l1.kt

// MODULE: L2
// MODULE_KIND: LibraryBinary
// FILE: l2.kt

// MODULE: G

// MODULE: E
// WILDCARD_MODIFICATION_EVENT

// MODULE: D

// MODULE: B(D, E)

// MODULE: F(L1, B)

// MODULE: C(F, G, L2)

// MODULE: A(B, C)
