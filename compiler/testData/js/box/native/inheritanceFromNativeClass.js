function A(a) {
    this.a = a;
}

A.prototype.g = function () {
    return 2 * this.a;
};

A.prototype.m = function () {
    return this.a - 1;
};

A.prototype.foo = function (i) {
    return "A.foo(" + i + ")";
};

A.prototype.boo = function (i) {
    return "A.boo(" + i + ")";
};

A.prototype.bar = function (i) {
    return "A.bar(" + i + ")";
}