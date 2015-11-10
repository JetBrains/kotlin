public class Testing {
    void f() {
        facades.MultiFileFacadeClass.<caret>
    }
}

// EXIST: funInFacade
// EXIST: funInFacade2
// LIGHT_CLASS: facades.MultiFileFacadeClass