package test;

class My {

    int foo(@local int i) {
        @local int j = i + 1;
        return j;
    }
}