// PRETTY_RENDERER_OPTION: FULLY_EXPANDED_TYPES

interface Base
typealias FirstAlias = Base
typealias SecondAlias = FirstAlias

fun foo<caret>(): SecondAlias {}
