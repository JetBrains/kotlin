// LATEST_LV_DIFFERENCE
// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +CompanionBlocksAndExtensions +CollectionLiterals
// WITH_STDLIB
// RENDER_DIAGNOSTIC_ARGUMENTS

class C

companion <!INAPPLICABLE_OPERATOR_MODIFIER("companion extension")!>operator<!> fun C.unaryPlus() = C()
companion <!INAPPLICABLE_OPERATOR_MODIFIER("companion extension")!>operator<!> fun C.unaryMinus() = C()
companion <!INAPPLICABLE_OPERATOR_MODIFIER("companion extension")!>operator<!> fun C.not() = C()

companion <!INAPPLICABLE_OPERATOR_MODIFIER("companion extension")!>operator<!> fun C.inc() = C()
companion <!INAPPLICABLE_OPERATOR_MODIFIER("companion extension")!>operator<!> fun C.dec() = C()

companion <!INAPPLICABLE_OPERATOR_MODIFIER("companion extension")!>operator<!> fun C.plus(other: Int) {}
companion <!INAPPLICABLE_OPERATOR_MODIFIER("companion extension")!>operator<!> fun C.minus(other: Int) {}
companion <!INAPPLICABLE_OPERATOR_MODIFIER("companion extension")!>operator<!> fun C.times(other: Int) {}
companion <!INAPPLICABLE_OPERATOR_MODIFIER("companion extension")!>operator<!> fun C.div(other: Int) {}
companion <!INAPPLICABLE_OPERATOR_MODIFIER("companion extension")!>operator<!> fun C.rem(other: Int) {}

companion <!INAPPLICABLE_OPERATOR_MODIFIER("companion extension")!>operator<!> fun C.rangeTo(other: Int) {}
companion <!INAPPLICABLE_OPERATOR_MODIFIER("companion extension")!>operator<!> fun C.rangeUntil(other: Int) {}

companion <!INAPPLICABLE_OPERATOR_MODIFIER("companion extension")!>operator<!> fun C.contains(value: Int): Boolean = false

companion <!INAPPLICABLE_OPERATOR_MODIFIER("companion extension")!>operator<!> fun C.get(index: Int) {}
companion <!INAPPLICABLE_OPERATOR_MODIFIER("companion extension")!>operator<!> fun C.set(index: Int, c: Char) {}

companion <!INAPPLICABLE_OPERATOR_MODIFIER("companion extension")!>operator<!> fun C.compareTo(other: C) = 1

companion <!INAPPLICABLE_OPERATOR_MODIFIER("companion extension")!>operator<!> fun C.iterator() {}
companion <!INAPPLICABLE_OPERATOR_MODIFIER("companion extension")!>operator<!> fun C.hasNext() = false
companion <!INAPPLICABLE_OPERATOR_MODIFIER("companion extension")!>operator<!> fun C.next() = 1

companion <!INAPPLICABLE_OPERATOR_MODIFIER("companion extension")!>operator<!> fun C.plusAssign(other: Int) {}
companion <!INAPPLICABLE_OPERATOR_MODIFIER("companion extension")!>operator<!> fun C.minusAssign(other: Int) {}
companion <!INAPPLICABLE_OPERATOR_MODIFIER("companion extension")!>operator<!> fun C.timesAssign(other: Int) {}
companion <!INAPPLICABLE_OPERATOR_MODIFIER("companion extension")!>operator<!> fun C.divAssign(other: Int) {}
companion <!INAPPLICABLE_OPERATOR_MODIFIER("companion extension")!>operator<!> fun C.remAssign(other: Int) {}

companion <!INAPPLICABLE_OPERATOR_MODIFIER("companion extension")!>operator<!> fun C.component1(): Int = 1

companion <!INAPPLICABLE_OPERATOR_MODIFIER("companion extension")!>operator<!> fun C.getValue(thisRef: Any?, property: kotlin.reflect.KProperty<*>) = 1
companion <!INAPPLICABLE_OPERATOR_MODIFIER("companion extension")!>operator<!> fun C.setValue(thisRef: Any?, property: kotlin.reflect.KProperty<*>, v: Any?) {}

companion <!INAPPLICABLE_OPERATOR_MODIFIER("companion extension")!>operator<!> fun C.provideDelegate(thisRef: Any?, property: kotlin.reflect.KProperty<*>) = lazy { 1 }

companion <!INAPPLICABLE_OPERATOR_MODIFIER("companion extension"), INAPPLICABLE_OPERATOR_MODIFIER("must not have an extension receiver")!>operator<!> fun C.of(vararg x: Int) = C()

companion operator fun C.invoke() {}

/* GENERATED_FIR_TAGS: funWithExtensionReceiver, functionDeclaration, integerLiteral, lambdaLiteral, nullableType,
operator, starProjection, thisExpression, unaryExpression */
