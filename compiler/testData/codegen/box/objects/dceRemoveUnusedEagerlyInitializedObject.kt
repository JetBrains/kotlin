// CHECK_OPTIMIZED_JS
// ONLY_IR_DCE

// CHECK_VARS_COUNT: name=UnusedTrivialObject$instance includeNestedDeclarations=true count=0

// Trivial object: no fields, no init blocks, only `Any` as a supertype, so it is eagerly initialized.
object UnusedTrivialObject {
    fun unusedTrivialMarker() = "unusedTrivialMarker"
}

fun box(): String = "OK"
