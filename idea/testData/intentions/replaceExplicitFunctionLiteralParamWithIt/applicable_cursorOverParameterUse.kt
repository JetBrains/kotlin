fun applyTwice<A>(f: (A) -> A, x: A) = f(f(x))
val x = applyTwice({ x -> <caret>x + 1 }, 40)
