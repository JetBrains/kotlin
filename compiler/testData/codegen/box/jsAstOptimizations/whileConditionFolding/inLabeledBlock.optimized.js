function box() {
    var i = 1;
    var sum = 0;
    outer: {
        while (i < 10) {
            sum += i;
            i++;
            if (sum > 20) break outer;
        }
    }

    if (sum != 21) return "fail: " + sum;

    return "OK"
}