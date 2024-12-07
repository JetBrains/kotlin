// DO_NOT_CHECK_NON_PSI_SYMBOL_RESTORE_K1
// PRETTY_RENDERER_OPTION: FULLY_EXPANDED_TYPES

interface Base
typealias FirstAlias = Base
typealias SecondAlias = FirstAlias

fun foo<caret>(): SecondAlias {}
