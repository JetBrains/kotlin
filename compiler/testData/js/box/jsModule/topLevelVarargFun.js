define("bar", [], function() {
    return function() {
        return "(" + Array.prototype.join.call(arguments, "") + ")";
    };
});