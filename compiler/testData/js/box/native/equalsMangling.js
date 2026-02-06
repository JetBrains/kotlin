function foo(first, second) {
    return first.equals(second);
}

function B(value) {
    this.value = value;
}
B.prototype.equals = function(other) {
    return other instanceof B && other.value == this.value;
};