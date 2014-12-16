interface SAM {
    String <caret>foo(Object s, int n);
}

class JTest {
    static void samTest(SAM sam) { }
}