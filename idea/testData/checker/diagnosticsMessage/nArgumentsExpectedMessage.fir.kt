// IGNORE_FIR

package name.that.may.be.very.long

import java.io.Serializable

interface Foo<T> : Comparable<Foo<T>>, Serializable, Cloneable

fun test(<warning>f</warning>: <error descr="[WRONG_NUMBER_OF_TYPE_ARGUMENTS] One type argument expected for interface Foo<T>">Foo</error>) {}
