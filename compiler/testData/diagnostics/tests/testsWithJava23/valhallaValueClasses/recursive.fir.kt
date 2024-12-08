// LANGUAGE: +ValhallaValueClasses
// IGNORE_BACKEND_K1: ANY
// RUN_PIPELINE_TILL: FRONTEND
// JVM_TARGET: 23

value class A(val y: Int, val z: A)
value class B(val y: Int, val z: B?)

value class C(val a: C?, val b: C, val c: C, val d: C?, val e: D)

value class D(val a: C)

<!INLINE_CLASS_DEPRECATED!>inline<!> class Inline1(val x: <!VALUE_CLASS_CANNOT_BE_RECURSIVE!>Inline1<!>)
<!INLINE_CLASS_DEPRECATED!>inline<!> class Inline2(val x: <!VALUE_CLASS_CANNOT_BE_RECURSIVE!>Inline2?<!>)
<!INLINE_CLASS_DEPRECATED!>inline<!> class Inline3(val x: <!VALUE_CLASS_CANNOT_BE_RECURSIVE!>Inline4<!>)
<!INLINE_CLASS_DEPRECATED!>inline<!> class Inline4(val x: <!VALUE_CLASS_CANNOT_BE_RECURSIVE!>Inline3?<!>)
