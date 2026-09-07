// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-80492
// LATEST_LV_DIFFERENCE
// ^^^ AbstractFirLightTreeDiagnosticsWithLatestLanguageVersionTest does not invoke Fir2IR and IR Lowerings
//     so cannot emit IR diagnostics

class MyList<T> {
    companion object {
        operator fun of(): MyList<Int> = MyList()
        tailrec operator fun of(vararg lams: Int): MyList<Int> {
            if (lams.size == 1) return []
            val x: MyList<Int> = [1, 2, 3]
            takeLst([1, 2, 3])
            return [lams[0]]
        }
    }
}

fun takeLst(lst: MyList<Int>) { }

/* GENERATED_FIR_TAGS: classDeclaration, companionObject, functionDeclaration, nullableType, objectDeclaration, operator,
suspend, typeParameter, vararg */
