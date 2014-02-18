package test

trait TraitWithFunImplement {
    fun foo(): Int {
        return 12;
    }
}

// SEARCH_TEXT: Trait
// REF: (test).TraitWithFunImplement