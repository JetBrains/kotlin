package sample

fun main() {
    A().foo()
    // fromLeft should be resolved, because 'left' comes first in dependencies order!
    A().fromLeft()
}