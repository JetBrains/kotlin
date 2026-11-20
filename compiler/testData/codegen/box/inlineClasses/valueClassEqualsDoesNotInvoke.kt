// ISSUE: KT-90675, KT-91448
// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// FILECHECK_STAGE: CStubs

OPTIONAL_JVM_INLINE_ANNOTATION
value class Wrapper(val value: UShort)

// `UShort` comes from the stdlib klib, so its `==` must be recognized as the one of an inline class after deserialization.
// CHECK-LABEL: define {{.*}}i1 @"kfun:#equalsUShort(kotlin.UShort;kotlin.UShort){}kotlin.Boolean"
// CHECK-NOT: -box>
// CHECK-NOT: #equals(kotlin.Any?){}kotlin.Boolean
// CHECK-LABEL: epilogue:
// CHECK_NOT_CALLED_IN_SCOPE: scope=equalsUShort function=equals
// CHECK_NEW_COUNT: function=equalsUShort count=0
fun equalsUShort(a: UShort, b: UShort): Boolean = a == b

// CHECK-LABEL: define {{.*}}i1 @"kfun:#equalsWrapper(Wrapper;Wrapper){}kotlin.Boolean"
// CHECK-NOT: -box>
// CHECK-NOT: #equals(kotlin.Any?){}kotlin.Boolean
// CHECK-LABEL: epilogue:
// CHECK_NOT_CALLED_IN_SCOPE: scope=equalsWrapper function=equals
// CHECK_NEW_COUNT: function=equalsWrapper count=0
fun equalsWrapper(a: Wrapper, b: Wrapper): Boolean = a == b

// CHECK-LABEL: define {{.*}}i16 @"kfun:#wrapAndUnwrap(kotlin.UShort){}kotlin.UShort"
// CHECK-NOT: -box>
// CHECK-NOT: -unbox>
// CHECK-LABEL: epilogue:
// CHECK_NEW_COUNT: function=wrapAndUnwrap count=0
fun wrapAndUnwrap(a: UShort): UShort = Wrapper(a).value

// Stdlib inline functions (e.g. `UShort.plus`) are deliberately not used here: their inlined bodies leave an `<UInt-unbox>` call
// in an unreachable block at this stage, which is irrelevant to this test.
// CHECK-LABEL: define {{.*}}i16 @"kfun:#select(kotlin.Boolean;Wrapper;Wrapper){}Wrapper"
// CHECK-NOT: -box>
// CHECK-NOT: -unbox>
// CHECK-LABEL: epilogue:
// CHECK_NEW_COUNT: function=select count=0
fun select(first: Boolean, a: Wrapper, b: Wrapper): Wrapper {
    val result = if (first) a else Wrapper(b.value)
    return result
}

// CHECK-LABEL: define ptr @"kfun:#box(){}kotlin.String"
fun box(): String {
    if (!equalsUShort(1u, 1u)) return "FAIL 1"
    if (equalsUShort(1u, 2u)) return "FAIL 2"
    if (!equalsWrapper(Wrapper(1u), Wrapper(1u))) return "FAIL 3"
    if (equalsWrapper(Wrapper(1u), Wrapper(2u))) return "FAIL 4"
    if (wrapAndUnwrap(42u) != 42u.toUShort()) return "FAIL 5"
    if (select(true, Wrapper(1u), Wrapper(2u)) != Wrapper(1u)) return "FAIL 6"
    if (select(false, Wrapper(1u), Wrapper(2u)) != Wrapper(2u)) return "FAIL 7"
    return "OK"
}
