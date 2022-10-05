fun test() {
    val string = <expr>(String)</expr>::class
}

// IGNORE_FE10
// In K2, references to types are not regarded as used, regardless of parentheses.

// IGNORE_FIR
// Does not pass because of KTIJ-23143