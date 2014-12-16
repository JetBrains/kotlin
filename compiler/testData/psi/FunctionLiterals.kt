fun foo() {
    {}

    {foo}

    {a -> a}

    {(a) -> a}
    {(a : A) -> a}
    {(a : A) : T -> a}
    {(a) : T -> a}

    {(a, a) -> a}
    {(a : A, a : B) -> a}
    {(a : A, a) : T -> a}
    {(a, a : B) : T -> a}

    {() -> a}
    {() -> a}
    {() : T -> a}
    {() : T -> a}

    {T.(a) -> a}
    {T.(a : A) -> a}
    {T.(a : A) : T -> a}
    {T.(a) : T -> a}

    {x, y -> 1}
    {[a] x, [b] y, [c] z -> 1}

    {((a: Int = object { fun t() {} }) -> Int).(x: Int) : String -> "" }
    { A.B<String>.(x: Int) -> }
    {((a: Boolean = true) -> Int).(x: Any) : Unit -> }
}
