// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +CompanionBlocksAndExtensions

class C {
    val a = b + d + g + i

    companion {
        val b = <!UNINITIALIZED_VARIABLE!>c<!>
        val c = 1

        val d = e
        const val e = 1

        const val f = <!UNINITIALIZED_VARIABLE!>g<!>
        const val g = 1

        val h = 1
        const val i = <!NON_CONST_VAL_USED_IN_CONSTANT_EXPRESSION!>h<!>
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, integerLiteral, propertyDeclaration */
