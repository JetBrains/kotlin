// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +ProperSupportOfInnerClassesInCallableReferenceLHS
// LANGUAGE: +CompanionBlocksAndExtensions

class Test<G> {
    inner class Inner {
        companion {
            fun foo() { }
        }
    }

    inner class GInner<D> {
        companion {
            fun foo() { }
        }
    }

    fun test() {
        Inner::foo
        Test.Inner::foo
        <!INVALID_QUALIFIER_IN_LHS_OF_CALLABLE_REFERENCE_TO_STATIC_ERROR!>Test<Int>.Inner<!>::foo

        GInner::foo
        <!INVALID_QUALIFIER_IN_LHS_OF_CALLABLE_REFERENCE_TO_STATIC_ERROR!>GInner<Int><!>::foo
        Test<Int>.<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!>GInner<!>::foo
        <!INVALID_QUALIFIER_IN_LHS_OF_CALLABLE_REFERENCE_TO_STATIC_ERROR!>Test<Int>.GInner<Int><!>::foo
    }
}

/* GENERATED_FIR_TAGS: callableReference, classDeclaration, functionDeclaration, nestedClass, nullableType,
typeParameter */
