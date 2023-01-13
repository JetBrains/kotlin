// !LANGUAGE: +UnrestrictedBuilderInference
// IGNORE_BACKEND_K2: JVM_IR, JS_IR, NATIVE
// FIR status: NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER on lazy call (Name3, T)
// WITH_STDLIB

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

//internal class TowerDataElementsForName4() {
//    @OptIn(ExperimentalStdlibApi::class)
//    val reversedFilteredLocalScopes = buildList l1@ {
//        class Foo {
//            val reversedFilteredLocalScopes by lazy(LazyThreadSafetyMode.NONE) {
//                @OptIn(ExperimentalStdlibApi::class)
//                buildList {
//                    for (i in lastIndex downTo 0) {
//                        add("")
//                        this@l1.add("")
//                    }
//                }
//            }
//        }
//    }
//}

fun box(): String {
    val x1 = TowerDataElementsForName().reversedFilteredLocalScopes
    val x2 = TowerDataElementsForName2().reversedFilteredLocalScopes
    val x3 = TowerDataElementsForName3().reversedFilteredLocalScopes
//    val x4 = TowerDataElementsForName4().reversedFilteredLocalScopes
    return "OK"
}
