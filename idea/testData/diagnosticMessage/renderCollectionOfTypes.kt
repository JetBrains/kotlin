// !DIAGNOSTICS_NUMBER: 1
// !DIAGNOSTICS: EXPECTED_PARAMETERS_NUMBER_MISMATCH

package aa

import java.util.ArrayList

fun foo(f: (List<Int>, ArrayList<String>) -> MutableMap<Int, String>) = f

fun test() {
    foo {
        ""
    }
}