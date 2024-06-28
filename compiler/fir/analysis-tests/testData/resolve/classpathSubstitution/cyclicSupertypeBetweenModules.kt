// MODULE: dependency
// FILE: dependency.kt
interface ToSubstitute

interface Intermediate : ToSubstitute

// MODULE: main(dependency)
// FILE: main.kt
interface ToSubstitute : <!CYCLIC_INHERITANCE_HIERARCHY!>Intermediate<!>

class Usage : Intermediate
