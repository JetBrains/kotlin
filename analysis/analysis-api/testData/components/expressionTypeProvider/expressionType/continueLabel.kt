fun test() {
    loop@ for (i in 1..2) {
        continue<expr>@loop</expr>
    }
}
