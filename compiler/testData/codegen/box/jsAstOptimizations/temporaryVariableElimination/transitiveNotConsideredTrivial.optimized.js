var global = 1;

function se() {
    return global++;
}

function box() {
    var $b = se();
    var result = $b + $b;
    if (result != 2) return "fail: " + result;

    return "OK";
}