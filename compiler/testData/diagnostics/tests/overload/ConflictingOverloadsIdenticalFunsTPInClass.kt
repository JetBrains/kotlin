class Aaa() {
    <!CONFLICTING_OVERLOADS!>fun f()<!> = 1
    <!CONFLICTING_OVERLOADS!>fun <P> f()<!> = 1
}
