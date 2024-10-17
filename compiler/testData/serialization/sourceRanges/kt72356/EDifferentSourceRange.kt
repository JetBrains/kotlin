fun <@Something X> bar() {}

// E has a fake override of foo(), which has annotation with const param having ANOTHER source range as @Something, in ANOTHER source file
class E : C()
