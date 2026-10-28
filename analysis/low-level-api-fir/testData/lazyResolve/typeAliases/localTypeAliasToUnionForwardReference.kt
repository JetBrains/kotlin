// LANGUAGE: +RichErrors, +LocalTypeAliases
error class E1
error class E2

// `TA` and `TS` are not resolved yet when the local type aliases are resolved.
// They have to be resolved lazily and not by the resolver for local declarations.
fun resolve<caret>Me() {
    typealias Loc = TA | E2
    typealias Loc2 = E1 | TS

    val a: Loc = E1()
}

typealias TA = E1
typealias TS = String
