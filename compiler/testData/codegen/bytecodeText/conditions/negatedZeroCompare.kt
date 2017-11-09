val a = 1

fun main() {
    if (!(a == 0)) {
        "then"
    } else {
        "else"
    }
}

//0 ICONST_0
//1 ICONST_1
//1 IFEQ
//0 IFNE
//1 IF
//1 GOTO