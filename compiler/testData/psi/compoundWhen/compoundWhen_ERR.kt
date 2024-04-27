fun foo() {
    when (val e = true; e) {
    }
    when (val e = true; e) {
        is  -> foo
        !is  -> foo
        in  -> foo
        !in  -> foo
        -> foo
        else
    }
    when (val e = true; e) {
        is  ->
        !is  ->
        in  ->
        !in  ->
        !in  -> ;
        ->
        else
        else ->
    }
    when (val e = true; e) {
        - -> foo
    }
}