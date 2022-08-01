fun test() {

    outer@while(true) {
        inner@while(false) {
            <expr>break@outer</expr>
        }
    }
}