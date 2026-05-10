// LANGUAGE: +CompanionBlocksAndExtensions +CollectionLiterals
// RUN_PIPELINE_TILL: BACKEND

class HiddenSetInObject {
    companion object {
        @Deprecated("", level = DeprecationLevel.HIDDEN)
        operator fun of(vararg x: Long): HiddenSetInObject = HiddenSetInObject()

        @Deprecated("", level = DeprecationLevel.HIDDEN)
        operator fun of(x: Long): HiddenSetInObject = HiddenSetInObject()
    }

    companion {
        operator fun of(vararg x: Int): HiddenSetInObject = HiddenSetInObject()
        operator fun of(x: Int): HiddenSetInObject = HiddenSetInObject()
    }
}

class HiddenSetInBlock {
    companion object {
        @Deprecated("", level = DeprecationLevel.HIDDEN)
        operator fun of(vararg x: Long): HiddenSetInBlock = HiddenSetInBlock()

        @Deprecated("", level = DeprecationLevel.HIDDEN)
        operator fun of(x: Long): HiddenSetInBlock = HiddenSetInBlock()
    }

    companion {
        operator fun of(vararg x: Int): HiddenSetInBlock = HiddenSetInBlock()
        operator fun of(x: Int): HiddenSetInBlock = HiddenSetInBlock()
    }
}

class HiddenPartsOfTheSameSetInBlock {
    companion {
        @Deprecated("", level = DeprecationLevel.HIDDEN)
        operator fun of() = HiddenPartsOfTheSameSetInBlock()
    }

    companion object {
        operator fun of(vararg i: Int) = HiddenPartsOfTheSameSetInBlock()
    }
}

class HiddenPartsOfTheSameSetInObject {
    companion object {
        @Deprecated("", level = DeprecationLevel.HIDDEN)
        operator fun of() = HiddenPartsOfTheSameSetInObject()
    }

    companion {
        operator fun of() = HiddenPartsOfTheSameSetInObject()
        operator fun of(vararg i: Int) = HiddenPartsOfTheSameSetInObject()
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, companionObject, functionDeclaration, objectDeclaration, operator,
stringLiteral, vararg */
