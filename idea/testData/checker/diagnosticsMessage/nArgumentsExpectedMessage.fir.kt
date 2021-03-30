package name.that.may.be.very.long

import java.io.Serializable

interface Foo<T> : Comparable<Foo<T>>, Serializable, Cloneable

fun test(f: <error descr="[WRONG_NUMBER_OF_TYPE_ARGUMENTS] One type argument expected for name/that/may/be/very/long/Foo">Foo</error>) {}
