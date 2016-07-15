class A

typealias <caret>F = (A) -> A

typealias G1 = (F) -> F
typealias G2 = F.() -> F
typealias G3 = F.(F) -> F