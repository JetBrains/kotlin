// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL
// DIAGNOSTICS: -UNUSED_PARAMETER
import kotlin.reflect.KProperty

@get:Anno(<!ANNOTATION_ARGUMENT_MUST_BE_CONST!>foo<!>)
@set:Anno(<!ANNOTATION_ARGUMENT_MUST_BE_CONST!>foo<!>)
@setparam:Anno(<!ANNOTATION_ARGUMENT_MUST_BE_CONST!>foo<!>)
@delegate:Anno(<!ANNOTATION_ARGUMENT_MUST_BE_CONST!>foo<!>)
var foo: Int by MyDelegate()

@Repeatable
annotation class Anno(val s: Int)

class MyDelegate {
    operator fun getValue(thisRef: Any?, property: KProperty<*>) = 42
    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: Int) {}
}
