fun foo() {
    class A1 constructor()
    class A2 Ann private constructor()

    class A3 private @Ann("") constructor()
    class A4 @Ann("") constructor()

    class A5
    Ann
    constructor()

    class A6
    Ann("")
    constructor()
}
