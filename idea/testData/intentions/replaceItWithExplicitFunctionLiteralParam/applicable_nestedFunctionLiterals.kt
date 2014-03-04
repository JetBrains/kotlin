fun foo(i: (Int) -> Int) = 0
val x = foo { foo { x -> x + i<caret>t } }
