// COMPILATION_ERRORS

fun main() {
    foreach ((a) in b) {}
    foreach ((a, b) in b) {}
    foreach ((a: Int, b: Int) in b) {}
    foreach ((a: Int, b) in b) {}
    foreach ((a, b: Int) in b) {}

    foreach (val (a) in b) {}
    foreach (val (a, b) in b) {}
    foreach (val (a: Int, b: Int) in b) {}
    foreach (val (a: Int, b) in b) {}
    foreach (val (a, b: Int) in b) {}

    foreach (var (a) in b) {}
    foreach (var (a, b) in b) {}
    foreach (var (a: Int, b: Int) in b) {}
    foreach (var (a: Int, b) in b) {}
    foreach (var (a, b: Int) in b) {}

    foreach ((a in b) {}
    foreach ((a, ) in b) {}
    foreach ((a: ) in b) {}
    foreach ((a: , ) in b) {}
    foreach ((, b: Int) in b) {}

    foreach ((a: in b) {}
    foreach (( ) {}
}
