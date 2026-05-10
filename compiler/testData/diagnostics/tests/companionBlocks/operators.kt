// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +CompanionBlocksAndExtensions +CollectionLiterals
// WITH_STDLIB
// RENDER_DIAGNOSTIC_ARGUMENTS

class C {
    companion {
        <!INAPPLICABLE_OPERATOR_MODIFIER("must be a member or an extension function")!>operator<!> fun unaryPlus() = C()
        <!INAPPLICABLE_OPERATOR_MODIFIER("must be a member or an extension function")!>operator<!> fun unaryMinus() = C()
        <!INAPPLICABLE_OPERATOR_MODIFIER("must be a member or an extension function")!>operator<!> fun not() = C()

        <!INAPPLICABLE_OPERATOR_MODIFIER("must be a member or an extension function")!>operator<!> fun inc() = C()
        <!INAPPLICABLE_OPERATOR_MODIFIER("must be a member or an extension function")!>operator<!> fun dec() = C()

        <!INAPPLICABLE_OPERATOR_MODIFIER("must be a member or an extension function")!>operator<!> fun plus(other: Int) {}
        <!INAPPLICABLE_OPERATOR_MODIFIER("must be a member or an extension function")!>operator<!> fun minus(other: Int) {}
        <!INAPPLICABLE_OPERATOR_MODIFIER("must be a member or an extension function")!>operator<!> fun times(other: Int) {}
        <!INAPPLICABLE_OPERATOR_MODIFIER("must be a member or an extension function")!>operator<!> fun div(other: Int) {}
        <!INAPPLICABLE_OPERATOR_MODIFIER("must be a member or an extension function")!>operator<!> fun rem(other: Int) {}

        <!INAPPLICABLE_OPERATOR_MODIFIER("must be a member or an extension function")!>operator<!> fun rangeTo(other: Int) {}
        <!INAPPLICABLE_OPERATOR_MODIFIER("must be a member or an extension function")!>operator<!> fun rangeUntil(other: Int) {}

        <!INAPPLICABLE_OPERATOR_MODIFIER("must be a member or an extension function")!>operator<!> fun contains(value: Int): Boolean = false

        <!INAPPLICABLE_OPERATOR_MODIFIER("must be a member or an extension function")!>operator<!> fun get(index: Int) {}
        <!INAPPLICABLE_OPERATOR_MODIFIER("must be a member or an extension function")!>operator<!> fun set(index: Int, c: Char) {}

        <!INAPPLICABLE_OPERATOR_MODIFIER("must be a member or an extension function")!>operator<!> fun compareTo(other: C) = 1

        <!INAPPLICABLE_OPERATOR_MODIFIER("must be a member or an extension function")!>operator<!> fun iterator() {}
        <!INAPPLICABLE_OPERATOR_MODIFIER("must be a member or an extension function")!>operator<!> fun hasNext() = false
        <!INAPPLICABLE_OPERATOR_MODIFIER("must be a member or an extension function")!>operator<!> fun next() = 1

        <!INAPPLICABLE_OPERATOR_MODIFIER("must be a member or an extension function")!>operator<!> fun plusAssign(other: Int) {}
        <!INAPPLICABLE_OPERATOR_MODIFIER("must be a member or an extension function")!>operator<!> fun minusAssign(other: Int) {}
        <!INAPPLICABLE_OPERATOR_MODIFIER("must be a member or an extension function")!>operator<!> fun timesAssign(other: Int) {}
        <!INAPPLICABLE_OPERATOR_MODIFIER("must be a member or an extension function")!>operator<!> fun divAssign(other: Int) {}
        <!INAPPLICABLE_OPERATOR_MODIFIER("must be a member or an extension function")!>operator<!> fun remAssign(other: Int) {}

        <!INAPPLICABLE_OPERATOR_MODIFIER("must be a member or an extension function")!>operator<!> fun component1(): Int = 1

        <!INAPPLICABLE_OPERATOR_MODIFIER("must be a member or an extension function")!>operator<!> fun getValue(thisRef: Any?, property: kotlin.reflect.KProperty<*>) = 1
        <!INAPPLICABLE_OPERATOR_MODIFIER("must be a member or an extension function")!>operator<!> fun setValue(thisRef: Any?, property: kotlin.reflect.KProperty<*>, v: Any?) {}

        <!INAPPLICABLE_OPERATOR_MODIFIER("must be a member or an extension function")!>operator<!> fun provideDelegate(thisRef: Any?, property: kotlin.reflect.KProperty<*>) = lazy { 1 }

        operator fun of(vararg x: Int) = C()

        operator fun invoke() {}
    }
}

/* GENERATED_FIR_TAGS: funWhExtensionReceiver, functionDeclaration, integerLiteral, lambdaLiteral, nullableType,
operator, starProjection, thisExpression, unaryExpression */
