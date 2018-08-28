// !LANGUAGE: +NewInference

fun bar() {
    if (true) <!TYPE_MISMATCH!>{
        <!EXPECTED_TYPE_MISMATCH!>fun local() {
        }<!>
    }<!> else {

    }
}
