// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-80492
// LANGUAGE: +CollectionLiterals, +ContextParameters
// RENDER_DIAGNOSTIC_ARGUMENTS

class Suspend {
    companion object {
        operator fun of(
            <!INCONSISTENT_PARAMETER_TYPES_IN_OF_OVERLOADS("() -> Int")!>sus: suspend () -> Int<!>,
            vararg rest: () -> Int,
        ): Suspend = Suspend()
    }
}

@Target(AnnotationTarget.TYPE)
annotation class Anno

class MyList {
    companion object {
        // probably, ofs like this shouldn't be allowed, see KT-83160
        operator fun of(
            p1: Int.() -> Int,
            p2: (x: Int) -> Int,
            p3: context(Int) () -> Int,
            p4: (@Anno Int) -> Int,
            p5: (Int) -> @Anno Int,
            p6: @Anno (Int) -> Int,
            vararg rest: (Int) -> Int,
        ): MyList = MyList()
    }
}

/* GENERATED_FIR_TAGS: annotationDeclaration, classDeclaration, companionObject, functionDeclaration, functionalType,
objectDeclaration, operator, suspend, typeWithContext, typeWithExtension, vararg */
