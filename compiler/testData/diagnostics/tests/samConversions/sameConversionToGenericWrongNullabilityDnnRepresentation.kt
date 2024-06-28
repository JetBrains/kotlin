// LANGUAGE: +JavaTypeParameterDefaultRepresentationWithDNN
// ISSUE: KT-57014
// FULL_JDK
// JVM_TARGET: 1.8

import java.util.function.Supplier

fun main() {
    val sam = Supplier<String> {
        foo()
    }

    val sam2: Supplier<String> = Supplier {
        <!TYPE_MISMATCH!>foo()<!>
    }

    val sam3 = object : Supplier<String> {
        override fun <!RETURN_TYPE_MISMATCH_ON_OVERRIDE!>get<!>() = foo()
    }
}

fun foo(): String? = null
