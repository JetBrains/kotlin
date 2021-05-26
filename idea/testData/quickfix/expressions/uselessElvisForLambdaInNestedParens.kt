// "Remove useless elvis operator" "true"
fun test() {
    ((({ "" } <caret>?: null)))
}
