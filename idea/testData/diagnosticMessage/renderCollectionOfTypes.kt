package aa

import java.util.ArrayList

fun foo(f: (List<Int>, ArrayList<String>) -> MutableMap<Int, String>) = f

fun test() {
    foo {
        ""
    }
}