fun test() {

    outer@while(true) {
        inner@while(false) {
            break<expr>@outer</expr>
        }
    }
}