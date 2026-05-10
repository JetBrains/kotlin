// LANGUAGE: +CompanionBlocksAndExtensions +CollectionLiterals
// RUN_PIPELINE_TILL: FRONTEND

class EmptyCompanionBlock {
    companion { }
    companion object {
        operator fun of(vararg x: String): EmptyCompanionBlock = EmptyCompanionBlock()
    }
}

class EmptyCompanionObject {
    companion {
        operator fun of(vararg x: String): EmptyCompanionObject = EmptyCompanionObject()
    }
    companion object
}

class TwoSetsOfSingleSame {
    companion {
        <!MULTIPLE_VARARG_OVERLOADS_OF_OPERATOR_OF!>operator fun of(vararg x: Int): <!UNRESOLVED_REFERENCE!>TwoSetsOfSingle<!><!> = <!UNRESOLVED_REFERENCE!>TwoSetsOfSingle<!>()
    }

    companion object {
        <!MULTIPLE_VARARG_OVERLOADS_OF_OPERATOR_OF!>operator fun of(vararg x: Int): TwoSetsOfSingleSame<!> = TwoSetsOfSingleSame()
    }
}

class TwoSetsOfSingleDifferent {
    companion object {
        <!MULTIPLE_VARARG_OVERLOADS_OF_OPERATOR_OF!>operator fun of(vararg x: Int): TwoSetsOfSingleDifferent<!> = TwoSetsOfSingleDifferent()
    }

    companion {
        <!MULTIPLE_VARARG_OVERLOADS_OF_OPERATOR_OF!>operator fun of(vararg x: Long): TwoSetsOfSingleDifferent<!> = TwoSetsOfSingleDifferent()
    }
}

class TwoSetsOfMultiple {
    companion object {
        <!MULTIPLE_VARARG_OVERLOADS_OF_OPERATOR_OF!>operator fun of(vararg x: Int): TwoSetsOfMultiple<!> = TwoSetsOfMultiple()
        operator fun of(): TwoSetsOfMultiple = TwoSetsOfMultiple()
    }

    companion {
        <!MULTIPLE_VARARG_OVERLOADS_OF_OPERATOR_OF!>operator fun of(vararg x: Long): TwoSetsOfMultiple<!> = TwoSetsOfMultiple()
        operator fun of(): TwoSetsOfMultiple = TwoSetsOfMultiple()
    }
}

class OneSetDistributed {
    companion object {
        operator fun of(vararg x: Int): OneSetDistributed = OneSetDistributed()
    }

    companion {
        <!OF_OVERLOADS_IN_BLOCK_AND_OBJECT!>operator fun of(): OneSetDistributed<!> = OneSetDistributed()
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, companionObject, functionDeclaration, objectDeclaration, operator, vararg */
