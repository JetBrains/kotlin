package test

class MyClass: Test() {

}

fun test(m: MyClass) {
    m.ac<caret>t {

    }
}

// REF: (in test.Test).act(Action)