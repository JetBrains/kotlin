// WITH_STDLIB
// The test depends on kotlin.jvm package
// TARGET_BACKEND: JVM
// MODULE: lib
// FILE: b.kt

package b

import a.*

public object MstLogicAlgebra1 : LogicAlgebra1<StringSymbol1> {
    override fun const(boolean: Boolean): StringSymbol1 = if (boolean) {
        LogicAlgebra1.TRUE
    } else {
        LogicAlgebra1.FALSE
    }
}

// FILE: a.kt

package a

import kotlin.properties.ReadOnlyProperty

public interface LogicAlgebra1<T : Any> {

    public fun const(boolean: Boolean): T

    public companion object {
        public val TRUE: StringSymbol1 by symbol1
        public val FALSE: StringSymbol1 by symbol1
    }
}

public val symbol1: ReadOnlyProperty<Any?, StringSymbol1> = ReadOnlyProperty { _, property ->
    StringSymbol1(property.name)
}

@JvmInline
public value class StringSymbol1(public val identity: String)

// MODULE: main(lib)
// FILE: c.kt

import b.*

fun box(): String {
    MstLogicAlgebra1.run { const(true) }
    return "OK"
}