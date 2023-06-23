// UNRESOLVED_REFERENCE
open class WithCompanionObject {
    companion object {
        fun fromCompanion() {}
    }
}

class Child : WithCompanionObject {
    /**
     * [fromComp<caret>anion]
     */
    fun usage() {}
}

