// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-86060
// LANGUAGE_FEATURE_TOGGLED: EagerLambdaAnalysis

fun testOnIterable() {
    [1, 2, 3].flatMap {
        [it]
    }
    [1, null, 3].flatMap {
        if (it == null) return@flatMap []
        [it]
    }
    [1, 2, 3].flatMap {
        if (it % 2 == 0) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.List<kotlin.Int>")!>[it]<!>
        else []
    }
    [1, 2, 3].flatMap {
        if (it % 2 == 0) [it]
        else listOf()
    }
    [1, 2, 3].flatMap {
        if (it % 2 == 0) return@flatMap []
        listOf(it)
    }
    [1, 2, 3].flatMap {
        if (it % 2 == 0) return@flatMap listOf()
        [it]
    }
    [1, 2, 3].flatMap {
        if (it % 2 == 0) [it]
        else sequenceOf()
    }
    [1, 2, 3].flatMap {
        if (it % 2 == 0) return@flatMap []
        sequenceOf(it)
    }
    [1, 2, 3].flatMap {
        if (it % 2 == 0) return@flatMap [it]
        listOf()
    }
}

fun testOnSequence() {
    sequenceOf(1, 2, 3).flatMap {
        [it]
    }
    sequenceOf(1, null, 3).flatMap {
        if (it == null) return@flatMap []
        [it]
    }
    sequenceOf(1, 2, 3).flatMap {
        if (it % 2 == 0) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.sequences.Sequence<kotlin.Int>")!>[it]<!>
        else []
    }
    sequenceOf(1, 2, 3).flatMap {
        if (it % 2 == 0) [it]
        else listOf()
    }
    sequenceOf(1, 2, 3).flatMap {
        if (it % 2 == 0) return@flatMap []
        listOf(it)
    }
    sequenceOf(1, 2, 3).flatMap {
        if (it % 2 == 0) return@flatMap listOf()
        [it]
    }
    sequenceOf(1, 2, 3).flatMap {
        if (it % 2 == 0) [it]
        else sequenceOf()
    }
    sequenceOf(1, 2, 3).flatMap {
        if (it % 2 == 0) return@flatMap []
        sequenceOf(it)
    }
    sequenceOf(1, 2, 3).flatMap {
        if (it % 2 == 0) return@flatMap [it]
        listOf()
    }
}

/* GENERATED_FIR_TAGS: equalityExpression, functionDeclaration, ifExpression, integerLiteral, lambdaLiteral,
multiplicativeExpression, nullableType, smartcast */
