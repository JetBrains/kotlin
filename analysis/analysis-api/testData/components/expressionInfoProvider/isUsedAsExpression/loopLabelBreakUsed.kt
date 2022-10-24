fun test() {

    outer@while(true) {
        inner@while(false) {
            val x = <expr>break@outer</expr>
        }
    }
}