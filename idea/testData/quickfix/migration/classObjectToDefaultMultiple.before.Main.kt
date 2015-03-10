// "Replace 'class' keyword with 'default' modifier in whole project" "true"

class A {
    public class<caret> object {

    }
}

class B {
    class object {

    }
}

class C {
    class object Named {

    }
}
