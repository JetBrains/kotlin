fun <             @Something X> bar() {}

// E has a fake override of foo(), which has annotation with const param having SAME source range as @Something, but in ANOTHER source file
class E : C()
