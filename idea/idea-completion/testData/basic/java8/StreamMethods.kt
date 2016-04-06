fun foo(x: List<String>) {
    x.stream().<caret>
}

// EXIST: {"lookupString":"allMatch","tailText":" {...} (((String!) -> Boolean)!)","typeText":"Boolean","attributes":"bold"}
// EXIST: {"lookupString":"allMatch","tailText":"(Predicate<in String!>!)","typeText":"Boolean","attributes":"bold"}
// EXIST: {"lookupString":"count","tailText":"()","typeText":"Long","attributes":"bold"}
