class Outer {
    public trait T: A

    public object O1: A()

    class Inner {
        public object O2: T
    }
}

