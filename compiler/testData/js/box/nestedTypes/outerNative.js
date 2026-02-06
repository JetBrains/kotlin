function A(x) {
    this.x = x;
}
A.prototype = {
    foo : function() {
        return this.x;
    }
};

A.B = function(value) {
    this.value = value;
};
A.B.prototype = {
    bar : function() {
        return 10000 + this.value;
    }
};

A.C = function(outer, value) {
    this.outer = outer;
    this.value = value;
};
A.C.prototype = {
    bar : function() {
        return this.outer.foo() + this.value + 10000;
    },
    dec : function() {
        this.outer.x--;
    }
};