// FIR_IDENTICAL
// LANGUAGE: +NameBasedDestructuring +DeprecateNameMismatchInShortDestructuringWithParentheses +EnableNameBasedDestructuringShortForm
// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-71772

fun test() {
    val resultA = pcla { otvOwner ->
        otvOwner.constrain(ScopeOwner())
        // should fix OTv := ScopeOwner for scope navigation
        val [valueL, valueR] = <!COMPONENT_FUNCTION_MISSING, COMPONENT_FUNCTION_MISSING!>otvOwner.provide()<!>
        // expected: Interloper </: ScopeOwner
        otvOwner.constrain(Interloper)
    }
    // expected: ScopeOwner
    resultA

    val resultB = pcla { otvOwner ->
        otvOwner.constrain(ScopeOwner())
        // should fix OTv := ScopeOwner for scope navigation
        val [valueL] = <!COMPONENT_FUNCTION_MISSING!>otvOwner.provide()<!>
        // expected: Interloper </: ScopeOwner
        otvOwner.constrain(Interloper)
    }
    // expected: ScopeOwner
    resultB

    val resultC = pcla { otvOwner ->
        otvOwner.constrain(ScopeOwner())
        // should fix OTv := ScopeOwner for scope navigation
        val [_, valueR] = <!COMPONENT_FUNCTION_MISSING, COMPONENT_FUNCTION_MISSING!>otvOwner.provide()<!>
        // expected: Interloper </: ScopeOwner
        otvOwner.constrain(Interloper)
    }
    // expected: ScopeOwner
    resultC

    val resultD = pcla { otvOwner ->
        otvOwner.constrain(ScopeOwner())
        // should fix OTv := ScopeOwner for scope navigation
        otvOwner.consumeLambda { <!COMPONENT_FUNCTION_MISSING, COMPONENT_FUNCTION_MISSING!>[valueL, valueR]<!> -> }
        // expected: Interloper </: ScopeOwner
        otvOwner.constrain(Interloper)
    }
    // expected: ScopeOwner
    resultD

    val resultE = pcla { otvOwner ->
        otvOwner.constrain(ScopeOwner())
        // should fix OTv := ScopeOwner for scope navigation
        otvOwner.consumeLambda { <!COMPONENT_FUNCTION_MISSING!>[valueL]<!> -> }
        // expected: Interloper </: ScopeOwner
        otvOwner.constrain(Interloper)
    }
    // expected: ScopeOwner
    resultE

    val resultF = pcla { otvOwner ->
        otvOwner.constrain(ScopeOwner())
        // should fix OTv := ScopeOwner for scope navigation
        otvOwner.consumeLambda { <!COMPONENT_FUNCTION_MISSING, COMPONENT_FUNCTION_MISSING!>[_, valueR]<!> -> }
        // expected: Interloper </: ScopeOwner
        otvOwner.constrain(Interloper)
    }
    // expected: ScopeOwner
    resultF

    val resultG = pcla { otvOwner ->
        otvOwner.constrain(ScopeOwner())
        // should fix OTv := ScopeOwner for scope navigation
        for ([valueL, valueR] in <!COMPONENT_FUNCTION_MISSING, COMPONENT_FUNCTION_MISSING!>otvOwner.provideIterable()<!>) {}
        // expected: Interloper </: ScopeOwner
        otvOwner.constrain(Interloper)
    }
    // expected: ScopeOwner
    resultG

    val resultH = pcla { otvOwner ->
        otvOwner.constrain(ScopeOwner())
        // should fix OTv := ScopeOwner for scope navigation
        for ([valueL] in <!COMPONENT_FUNCTION_MISSING!>otvOwner.provideIterable()<!>) {}
        // expected: Interloper </: ScopeOwner
        otvOwner.constrain(Interloper)
    }
    // expected: ScopeOwner
    resultH

    val resultI = pcla { otvOwner ->
        otvOwner.constrain(ScopeOwner())
        // should fix OTv := ScopeOwner for scope navigation
        for ([_, valueR] in <!COMPONENT_FUNCTION_MISSING, COMPONENT_FUNCTION_MISSING!>otvOwner.provideIterable()<!>) {}
        // expected: Interloper </: ScopeOwner
        otvOwner.constrain(Interloper)
    }
    // expected: ScopeOwner
    resultI
}


class TypeVariableOwner<T> {
    fun constrain(subtypeValue: T) {}
    fun provide(): T = null!!
    fun consumeLambda(lambda: (T) -> Unit) {}
    fun provideIterable(): Iterable<T> = null!!
}

fun <OT> pcla(lambda: (TypeVariableOwner<OT>) -> Unit): OT = null!!

interface BaseType

object Value

class ScopeOwner: BaseType {
    operator fun component1(): Value = Value
    operator fun component2(): Value = Value
}

object Interloper: BaseType

/* GENERATED_FIR_TAGS: checkNotNullCall, classDeclaration, destructuringDeclaration, forLoop, functionDeclaration,
functionalType, interfaceDeclaration, lambdaLiteral, localProperty, nullableType, objectDeclaration, operator,
propertyDeclaration, typeParameter, unnamedLocalVariable */
