// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE: +UnrestrictedBuilderInference +ForbidInferringPostponedTypeVariableIntoDeclaredUpperBound
// ISSUE: KT-56169

internal class TowerDataElementsForName() {
    val reversedFilteredLocalScopes by lazy(LazyThreadSafetyMode.NONE) {
        @OptIn(ExperimentalStdlibApi::class)
        buildList {
            for (i in lastIndex downTo 0) {
                add("")
            }
        }
    }
}

internal class TowerDataElementsForName2() {
    @OptIn(ExperimentalStdlibApi::class)
    val reversedFilteredLocalScopes = buildList {
        val reversedFilteredLocalScopes by lazy(LazyThreadSafetyMode.NONE) {
            @OptIn(ExperimentalStdlibApi::class)
            buildList {
                for (i in lastIndex downTo 0) {
                    add("")
                }
            }
        }
        add(reversedFilteredLocalScopes)
    }
}

internal class TowerDataElementsForName3() {
    val reversedFilteredLocalScopes by lazy(LazyThreadSafetyMode.NONE) {
        @OptIn(ExperimentalStdlibApi::class)
        buildList l1@ {
            for (i in lastIndex downTo 0) {
                val reversedFilteredLocalScopes by lazy(LazyThreadSafetyMode.NONE) {
                    @OptIn(ExperimentalStdlibApi::class)
                    buildList {
                        for (i in lastIndex downTo 0) {
                            add("")
                            this@l1.add("")
                        }
                    }
                }
            }
        }
    }
}

internal class TowerDataElementsForName4() {
    @OptIn(ExperimentalStdlibApi::class)
    val reversedFilteredLocalScopes = buildList l1@ {
        class Foo {
            val reversedFilteredLocalScopes by lazy(LazyThreadSafetyMode.NONE) {
                @OptIn(ExperimentalStdlibApi::class)
                buildList {
                    for (i in lastIndex downTo 0) {
                        add("")
                        this@l1.add("")
                    }
                }
            }
        }
    }
}
