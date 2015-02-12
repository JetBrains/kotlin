// "Change getter type to Module" "true"

import kotlin.modules.Module

class A() {
    val i: kotlin.modules.Module
        get(): <caret>Module = kotlin.modules.ModuleBuilder("", "")
}
