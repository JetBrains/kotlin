var value = "OK";

function foo(newValue) {
    var $tmp = value;
    value = newValue;
    return $tmp;
}

function box() {
    return foo("fail");
}