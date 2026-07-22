var global = "";

function foo(x) {
    global += x;
    return x;
}

function box() {
    var i = 0;
    do {
        ++i;
        if (i == 5) {
            continue
        }
        global += ";";
        if (foo(i) >= 10) {
            break;
        }
    } while (true);

    if (global != ";1;2;3;4;6;7;8;9;10") return "fail: " + global;

    return "OK"
}