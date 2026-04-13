// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: -ReportDeprecationsOfOuterImportedClasses
// FILE: my.kt
package my

@Deprecated("", level = DeprecationLevel.ERROR)
class ForError {
    class Nested1 {
        class Nested2 {
            class Nested3
        }
    }
}

@Deprecated("", level = DeprecationLevel.HIDDEN)
class ForHidden {
    class Nested4 {
        class Nested5 {
            class Nested6
        }
    }
}

// FILE: test.kt
import <!DEPRECATION_ERROR!>my.ForError.Nested1<!>
import <!DEPRECATION_OF_OUTER_CLASS!>my.ForError.Nested1.Nested2<!>
import <!DEPRECATION_OF_OUTER_CLASS!>my.ForError.Nested1.Nested2.Nested3<!>
import <!DEPRECATION_ERROR!>my.ForHidden.Nested4<!>
import <!DEPRECATION_OF_OUTER_CLASS!>my.ForHidden.Nested4.Nested5<!>
import <!DEPRECATION_OF_OUTER_CLASS!>my.ForHidden.Nested4.Nested5.Nested6<!>

/* GENERATED_FIR_TAGS: classDeclaration, nestedClass, stringLiteral */
