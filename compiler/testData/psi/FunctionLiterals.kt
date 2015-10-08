fun foo() {
    {}

    {foo}

    {a -> a}

    {x, y -> 1}

    {a: b -> f}
    {a: b, c -> f}
    {a: b, c: d -> f}
    {a: (Int) -> Unit, c: (Int) -> Unit -> f}

    //{a: ((Int) -> Unit) ->} todo
    //{[a] a: A -> }
}
