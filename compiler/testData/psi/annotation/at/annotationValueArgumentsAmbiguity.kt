fun foo() {
    @ann ({ it -> it + 1}) // lambda parsed as argument of annotation, and annotated expression is "print(1)"
    print(1)

    @ann() ({ it -> it + 1}) // lambda in parenthesises annotated, "print(1)" is separated expression
    print(1)
}
