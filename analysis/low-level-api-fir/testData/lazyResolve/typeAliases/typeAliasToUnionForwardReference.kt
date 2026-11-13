// LANGUAGE: +RichErrors
error class E1
error class E2

// `TA` is declared later, so it has to be resolved on demand to classify it as a rich error component
typealias Resolve<caret>Me = TA | E2
typealias TA = E1
