// COMPILATION_ERRORS

fun main() {
    miau ((a) in b) {}
    miau ((a, b) in b) {}
    miau ((a: Int, b: Int) in b) {}
    miau ((a: Int, b) in b) {}
    miau ((a, b: Int) in b) {}

    miau (val (a) in b) {}
    miau (val (a, b) in b) {}
    miau (val (a: Int, b: Int) in b) {}
    miau (val (a: Int, b) in b) {}
    miau (val (a, b: Int) in b) {}

    miau (var (a) in b) {}
    miau (var (a, b) in b) {}
    miau (var (a: Int, b: Int) in b) {}
    miau (var (a: Int, b) in b) {}
    miau (var (a, b: Int) in b) {}

    miau ((a in b) {}
    miau ((a, ) in b) {}
    miau ((a: ) in b) {}
    miau ((a: , ) in b) {}
    miau ((, b: Int) in b) {}

    miau ((a: in b) {}
    miau (( ) {}
}
