fun foo() {
    testtest<foofoo,
             testtest<testtest<
                 foofoo,
             >,
             >,
             testsa,
    >()

    testtest<foofoo, foofoo, foofoo,
             foofoo, bar>()

    testtest<
        foofoo, foofoo, foofoo, foofoo, bar
    >()

    testtest<foofoo, foofoo, foofoo, foofoo, bar
    >()

    testtest<foofoo, foofoo, foofoo, foofoo,
             bar
    >()

    testtest<foofoo
    >()

    testtest<
        foofoo>()

    testtest<
        foofoo
    >()

    testtest<foofoo,>()

    testtest<foofoo, testtest<testtest<foofoo>>>()

    testtest<foofoo, fososos, testtest<testtest<foofoo>>,>()

    testtest<foofoo, fososos, testtest<testtest<foofoo>>
            ,>()

    testtest<foofoo, testtest<testtest<foofoo,>>, testsa>()

    testtest<foofoo, *, testtest<testtest<foofoo,>>, testsa>()

    testtest<
        foofoo, foofoo, foofoo, foofoo,
        bar /*
    */, /* */ foo
    >()

    testtest</*
    */foofoo, foofoo, foofoo, /*

    */
      foofoo, bar>()

    testtest<foofoo, foofoo, foofoo, foofoo, bar/*
    */>()

    testtest<foofoo, foofoo, foofoo, foofoo, bar // awdawda
    >()

    testtest<foofoo, foofoo, foofoo, foofoo, /*

    */
             bar
    >()

    testtest<foofoo // fd
    >()

    testtest< /**/
        foofoo
    >()

    testtest<foofoo,/**/>()

    testtest<foofoo, foofoo, foofoo, foofoo/*
     */ , /* */ bar
    >()

    testtest<foofoo // fd
    >()

    testtest< /**/
        foofoo
    >()

    testtest<foofoo,/**/>()

    testtest<foofoo,/*
    */>()

    testtest<
            foofoo,/**/>()

    testtest<foofoo, fososos,/*
    */ testtest<testtest<foofoo>>,>()

    testtest<foofoo, testtest<testtest<foofoo,>>, /**/testsa>()

    testtest<foofoo, testtest<testtest<foofoo,>>/* */ , /**/testsa>()

    testtest<foofoo, testtest<testtest<foofoo,>>/*
    */ ,testsa>()

    testtest<foofoo, seee, testtest<testtest<foofoo,>>, /**/testsa>()

    testtest<foofoo, seee, testtest<testtest<foofoo,>>, /*
    */testsa>()
}