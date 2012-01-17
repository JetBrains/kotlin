package test

import java.lang.CharSequence

val Int.ggg: CharSequence
    get() = throw Exception()
