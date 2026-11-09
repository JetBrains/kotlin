// WITH_STDLIB
// DUMP_IR_AFTER_INLINE
// ISSUE: KT-91111
// LANGUAGE: -IrRichCallableReferencesInKlibs -IrIntraModuleInlinerBeforeKlibSerialization -IrCrossModuleInlinerBeforeKlibSerialization
// The language features are those of -language-version 2.2: callable references are not upgraded to rich ones before KLIB
// serialization, and there is no IR inlining before it. Compilers which generated legacy IrFunctionReference and
// IrPropertyReference in this configuration kept references to fake overrides of local classes in KLIBs, as
// AvoidLocalFakeOverridesLowering does not handle legacy references. The dump after the 1st stage checks that no reference
// to a local fake override remains: references are expected to be rich, with reflection targets in the supertypes.

open class Base {
    fun foo(): String = "foo"
    val bar: String get() = "bar"
    var baz: String = "baz"
}

interface WithDefaults {
    fun qux(): String = "qux"
    val quux: String get() = "quux"
}

fun boundReferences(): String {
    val obj = object : Base(), WithDefaults {}
    val function = obj::foo
    val property = obj::bar
    val mutableProperty = obj::baz
    val interfaceFunction = obj::qux
    val interfaceProperty = obj::quux
    return function() + property.get() + mutableProperty.get() + interfaceFunction() + interfaceProperty.get()
}

fun unboundReferences(): String {
    class Local : Base(), WithDefaults

    val function = Local::foo
    val property = Local::bar
    val interfaceFunction = Local::qux
    return function(Local()) + property.get(Local()) + interfaceFunction(Local())
}

// For comparison: calls to the same fake overrides
fun calls(): String {
    val obj = object : Base(), WithDefaults {}
    return obj.foo() + obj.bar + obj.baz + obj.qux() + obj.quux
}
