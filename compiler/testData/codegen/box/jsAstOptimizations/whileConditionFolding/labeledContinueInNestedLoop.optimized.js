var global = "";

function foo(x) {
    global += x;
    return x;
}

function box() {
    var i = 0;
    var j;
    loop: do {
        ++i;
        global += ";";
        for (j = 0; j < 2; ++j) {
            if (j == 1 && i == 2) {
                continue loop;
            }
            global += "-";
        }
        if (foo(i) >= 5) {
            break;
        }
    } while (true);

    if (global != ";--1;-;--3;--4;--5") return "fail: " + global;

    return "OK"
}