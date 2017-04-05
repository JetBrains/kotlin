package test;

class InterfaceImpl implements Interface {

    interface BuilderImpl extends Interface.Builder {
        @Override
        Builder setKind(Kind kind);
    }

    Builder getBuilder() { return null; }

}

interface Interface {

    enum Kind { DECLARATION; }

    interface Builder {
        Builder setKind(Kind kind);
    }

}
