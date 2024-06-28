// MODULE: L1
// MODULE_KIND: LibraryBinary
// WILDCARD_MODIFICATION_EVENT
// FILE: l1.kt

// MODULE: L2
// MODULE_KIND: LibraryBinary
// FILE: l2.kt

// MODULE: D(L1)

// MODULE: C(D, L2)

// MODULE: B(D)

// MODULE: A(B, C)
