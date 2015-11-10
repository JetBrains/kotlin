package test;

public class Testing {
    void f() {
        facades.Multi<caret>
    }
}

// EXIST: MultiFileFacadeClass
// NUMBER: 1

