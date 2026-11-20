// LANGUAGE: +RichErrors
error class E1

// `TB` is declared later, so it has to be resolved on demand to detect the non-error component in the wrong position
typealias Resolve<caret>Me = E1 | TB
typealias TB = String
