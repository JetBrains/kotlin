fun foo() {
    { -> a}

    {(a -> a}
    {(a : ) -> a}

    {(a, ) -> a}
    {(a : A, , a : B) -> a}
    {(a : A, , , a) : T -> a}

    {T.t(a) -> a}
    {T.t -(a : A) -> a}

    {a : b, -> f}
    {a : , c -> f}
    {a :  -> f}
    {a, -> f}

    {a : b, }
    {a : , }

    {T.a : b -> f}

    {(a, b) }
    {T.(a, b) }
    {(a: Int, )}
    {a, }

    {() -> a}
    {() -> a}
    {() : T -> a}
    {() : T -> a}

    {T.(a) -> a}
    {T.(a : A) -> a}
    {T.(a : A) : T -> a}
    {T.(a) : T -> a}

    {@[a] x, @[b] y, @[c] z -> 1}

    {((a: Int = object { fun t() {} }) -> Int).(x: Int) : String -> "" }
    { A.B<String>.(x: Int) -> }
    {((a: Boolean = true) -> Int).(x: Any) : Unit -> }
}
