fun test() {

    <expr>outer@</expr>while(true) {
        inner@while(false) {
            break@outer
        }
    }
}