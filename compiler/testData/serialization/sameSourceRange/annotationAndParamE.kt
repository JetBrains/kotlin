fun <             @Something X> bar() {}

// E has a fake override of foo(), which has annotation with const param having same source range as @Something, but in another source file
class E : C()
