class A {
    operator fun A.minus(o: A) = o

    <!INAPPLICABLE_OPERATOR_MODIFIER!>operator<!> fun A.add(o: A) = o
    operator fun A.get(o: A) = o
    <!INAPPLICABLE_OPERATOR_MODIFIER!>operator<!> fun A.invokee() {}
}

operator fun A.plus(o: A) = o
operator fun A.component1() = 1
<!INAPPLICABLE_OPERATOR_MODIFIER!>operator<!> fun A.componentN() = 1