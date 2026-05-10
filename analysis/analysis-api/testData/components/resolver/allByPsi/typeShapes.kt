typealias StringList = List<String>

class Outer {
    class Nested
}

interface Logger
interface Database

fun simple(x: String) {}

fun generic(x: List<String>) {}

fun nested(x: Outer.Nested) {}

fun nestedQualified(x: kotlin.Int.Companion) {}

fun typeAlias(x: StringList) {}

fun <T> typeParameter(x: T) {}

fun <T> typeParameterNullable(x: T?) {}

fun nullable(x: String?, y: Unresolved?) {}

fun functionType(x: (Int, String) -> Boolean, y: (Long, Unresolved) -> Unit) {}

fun functionTypeNoArg(x: () -> Unit) {}

fun functionTypeWithReceiver(x: Int.(String) -> Unit) {}

fun suspendFunctionType(x: suspend () -> Unit) {}

fun functionTypeWithContext(x: context(Logger, Database) (String) -> Unit) {}

fun nullableFunctionType(x: ((Int) -> Int)?) {}

fun <T> intersectionType(x: T & Any) {}

fun returnType(): String = ""

val propertyType: String = ""
