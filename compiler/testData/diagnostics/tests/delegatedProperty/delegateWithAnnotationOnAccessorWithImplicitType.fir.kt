// !DIAGNOSTICS: -UNUSED_PARAMETER
// IGNORE_REVERSED_RESOLVE
import kotlin.reflect.KProperty

@get:Anno(<!ANNOTATION_ARGUMENT_MUST_BE_CONST, TYPECHECKER_HAS_RUN_INTO_RECURSIVE_PROBLEM!>foo<!>)
@set:Anno(<!ANNOTATION_ARGUMENT_MUST_BE_CONST!>foo<!>)
@setparam:Anno(<!ANNOTATION_ARGUMENT_MUST_BE_CONST!>foo<!>)
@delegate:Anno(<!ANNOTATION_ARGUMENT_MUST_BE_CONST!>foo<!>)
var foo by MyDelegate()

@Repeatable
annotation class Anno(val i: Int)

class MyDelegate {
    operator fun getValue(thisRef: Any?, property: KProperty<*>) = 42
    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: Int) {}
}
