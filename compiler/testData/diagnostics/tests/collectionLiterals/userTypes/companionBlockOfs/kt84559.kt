// LANGUAGE: +CollectionLiterals +CompanionBlocksAndExtensions
// RUN_PIPELINE_TILL: FRONTEND
// WITH_STDLIB

@RequiresOptIn("", level = RequiresOptIn.Level.ERROR)
annotation class MyExperimental

class C {
    companion {
        @MyExperimental
        operator fun of(vararg i: Int) = C()
    }
}

fun test() {
    val a: C = @OptIn(MyExperimental::class) [1, 2, 3]
    val b: C = @<!UNRESOLVED_REFERENCE!>Unresolved<!> <!OPT_IN_USAGE_ERROR!>[1, 2, 3]<!>
}

/* GENERATED_FIR_TAGS: annotationDeclaration, classDeclaration, functionDeclaration, integerLiteral, localProperty,
operator, propertyDeclaration, stringLiteral, vararg */
