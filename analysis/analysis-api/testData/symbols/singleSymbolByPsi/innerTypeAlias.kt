// DO_NOT_CHECK_NON_PSI_SYMBOL_RESTORE_K1

class Outer<A, B> {
    inner type<caret>alias MyAlias<C, D> = Map<Map<A, B>, Map<C, D>>
}
