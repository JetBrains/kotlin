fun foo() {
    (<caret>1    + // abc
        2 /*def*/)   * 3
}

/*
1
1 + 2
(1 + 2) * 3
*/