class C {
    val names: List<String>                            // clearly tells the type of the property upfront
    field: MutableList<String> = <expr>mutableListOf()</expr>
}

// IGNORE_FE10
// RHS of initializer is deemed unused by FE1.0