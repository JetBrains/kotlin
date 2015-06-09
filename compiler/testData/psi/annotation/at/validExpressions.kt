fun foo() {
    return @ann 1
    return (@ann 2)

    @ann foo("")

    @ann 3 + @ann 4 * @ann("") 5 infix @ann 6

    foo.bar(@ann fun(x: Int) {

    })

    @ann if (@ann true || true) {

    }
    else {}

    for (i in @ann x) {}

    label@ @ann while (true) {
        @ann break@label
    }

    return@label @ann 1

    label@simpleName

    val x = @ann @[ann] l@{
        a, b, c -> a
    }
}
