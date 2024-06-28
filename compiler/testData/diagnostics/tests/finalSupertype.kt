// FIR_IDENTICAL
// ISSUE: KT-59830

class OOO

typealias Alias = OOO

class Child : <!FINAL_SUPERTYPE!>Alias<!>() //K1: [FINAL_SUPERTYPE] This type is final, so it cannot be inherited from, no error in K2
