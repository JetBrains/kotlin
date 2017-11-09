class Some {
    operator fun <caret>() {

    }
}

// EXIST: {"lookupString":"compareTo","typeText":"< > <= >="}
// EXIST: {"lookupString":"contains","typeText":"in !in"}
// EXIST: {"lookupString":"dec","typeText":"--"}
// EXIST: {"lookupString":"div","typeText":"/"}
// EXIST: {"lookupString":"divAssign","typeText":"/="}
// EXIST: {"lookupString":"equals","typeText":"== !="}
// EXIST: {"lookupString":"get","typeText":"[...]"}
// EXIST: {"lookupString":"getValue"}
// EXIST: {"lookupString":"hasNext"}
// EXIST: {"lookupString":"inc","typeText":"++"}
// EXIST: {"lookupString":"invoke","typeText":"(...)"}
// EXIST: {"lookupString":"iterator"}
// EXIST: {"lookupString":"minus","typeText":"-"}
// EXIST: {"lookupString":"minusAssign","typeText":"-="}
// EXIST: {"lookupString":"next"}
// EXIST: {"lookupString":"not","typeText":"!"}
// EXIST: {"lookupString":"plus","typeText":"+"}
// EXIST: {"lookupString":"plusAssign","typeText":"+="}
// EXIST: {"lookupString":"rangeTo","typeText":".."}
// EXIST: {"lookupString":"rem","typeText":"%"}
// EXIST: {"lookupString":"remAssign","typeText":"%="}
// EXIST: {"lookupString":"set","typeText":"[...] = ..."}
// EXIST: {"lookupString":"setValue"}
// EXIST: {"lookupString":"times","typeText":"*"}
// EXIST: {"lookupString":"timesAssign","typeText":"*="}
// EXIST: {"lookupString":"unaryMinus","typeText":"-"}
// EXIST: {"lookupString":"unaryPlus","typeText":"+"}
// NOTHING_ELSE