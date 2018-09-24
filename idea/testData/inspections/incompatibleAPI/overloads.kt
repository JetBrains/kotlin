package problem.api.kotlin.overloads

import lib.LibMethods

fun ktOverloads(lib: LibMethods) {
    lib.overload1(12)
    lib.overload1("Some")

    lib.overload2(12)
    lib.overload2("Some")
}