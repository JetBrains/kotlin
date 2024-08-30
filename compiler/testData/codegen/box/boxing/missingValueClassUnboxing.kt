// ISSUE: KT-69408
// DONT_TARGET_EXACT_BACKEND: JVM
// DONT_TARGET_EXACT_BACKEND: JVM_IR
// REASON: For value classes, JVM backend needs usage of @kotlin.jvm.JvmInline annotation, which does not present in other backends
// The goal of this testcase is to test behavior of IR Inliner, which is not being used in JVM backends

// To reproduce problem, in file compiler/ir/ir.inline/src/org/jetbrains/kotlin/ir/inline/FunctionInlining.kt,
// change default value of `private val insertAdditionalImplicitCasts: Boolean` to `false`,
// so JS backend will incorrectly miss value class unboxing, which is normally done in `AutoboxingTransformer.useExpressionAsType()`:

// Expected :OK
// Actual   :MyResult(myValue=OK)

// Correct JS code would be:
//   function box() {
//    var foo = box$lambda;
//    // Inline function 'MyResult.myGet' call
//    var this_0 = foo().myValue_1;
//    return _MyResult___get_myValue__impl__6ju6aw(this_0);
//  }
//  function _MyResult___get_myValue__impl__6ju6aw($this) {
//    return $this;
//  }

// Wrong JS code misses unboxing (getting field `.myValue_1` from `foo()` result):
//   function box() {
//    var foo = box$lambda;
//    // Inline function 'MyResult.myGet' call
//    var this_0 = foo();
//    return _MyResult___get_myValue__impl__6ju6aw(this_0);
//  }
//  function _MyResult___get_myValue__impl__6ju6aw($this) {
//    return $this;
//  }

// In correct IR, AutoboxingTransformer inserts the following instead of IMPLICIT_CAST:
//   CALL 'internal final fun unboxIntrinsic <T, R> (x: T of kotlin.js.unboxIntrinsic): R of kotlin.js.unboxIntrinsic declared in kotlin.js' type=<root>.MyResult<T of <root>.MyResult> origin=SYNTHESIZED_STATEMENT
// In incorrect IR, this cast is REINTERPRET_CAST, which probably is inserted by TypeOperatorLowering, and causes disruption to autoboxer

value class MyResult<out T> constructor(
    val myValue: T
) {
    inline fun myGet(): T = myValue
}

fun box(): String {
    val foo: ()->MyResult<String> = { MyResult("OK") }
    return foo().myGet()
}
